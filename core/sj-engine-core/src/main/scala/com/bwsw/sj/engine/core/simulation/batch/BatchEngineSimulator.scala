/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.engine.core.simulation.batch

import com.bwsw.sj.common.engine.core.batch.{BatchCollector, BatchStreamingExecutor, WindowRepository}
import com.bwsw.sj.common.engine.core.entities._
import com.bwsw.sj.common.si.model.instance.BatchInstance
import com.bwsw.sj.engine.core.simulation.state.{CommonEngineSimulator, ModuleEnvironmentManagerMock, SimulationResult}
import org.mockito.Mockito.{mock, when}

import scala.collection.mutable

/**
  * Simulates behavior of [[com.bwsw.sj.common.engine.TaskEngine TaskEngine]] for testing an implementation of
  * [[BatchStreamingExecutor]]
  *
  * Usage example:
  * {{{
  * val stateSaver = mock(classOf[StateSaverInterface])
  * val stateLoader = new StateLoaderMock
  * val stateService = new RAMStateService(stateSaver, stateLoader)
  * val stateStorage = new StateStorage(stateService)
  * val options = ""
  * val output = new TStreamStreamDomain("out", mock(classOf[TStreamServiceDomain]), 3, tags = Array("output"))
  * val manager = new ModuleEnvironmentManagerMock(stateStorage, options, Array(output))
  * val executor: BatchStreamingExecutor[String] = new SomeExecutor(manager)
  * val tstreamInput = new TStreamStreamDomain("t-stream-input", mock(classOf[TStreamServiceDomain]), 1)
  * val kafkaInput = new KafkaStreamDomain("kafka-input", mock(classOf[KafkaServiceDomain]), 1, 1)
  * val inputs = Array(tstreamInput, kafkaInput)
  * *
  * val batchInstanceDomain = mock(classOf[BatchInstanceDomain])
  * when(batchInstanceDomain.getInputsWithoutStreamMode).thenReturn(inputs.map(_.name))
  *
  * val batchCollector = new SomeBatchCollector(batchInstanceDomain, mock(classOf[BatchStreamingPerformanceMetrics]), inputs)
  *
  * val simulator = new BatchEngineSimulator(executor, manager, batchCollector)
  * simulator.prepareState(Map("idleCalls" -> 0, "symbols" -> 0))
  * simulator.prepareTstream(Seq("ab", "c", "de"), tstreamInput.name)
  * simulator.prepareKafka(Seq("fgh", "g"), kafkaInput.name)
  * simulator.prepareTstream(Seq("ijk", "lm"), tstreamInput.name)
  * simulator.prepareTstream(Seq("n"), tstreamInput.name)
  * simulator.prepareTstream(Seq("o"), tstreamInput.name)
  * simulator.prepareKafka(Seq("p", "r", "s"), kafkaInput.name)
  *
  * val batchesNumberBeforeIdle = 2
  * val window = 3
  * val slidingInterval = 1
  * val results = simulator.process(batchesNumberBeforeIdle, window, slidingInterval)
  *
  * println(results)
  * }}}
  *
  * @param executor       implementation [[BatchStreamingExecutor]] under test
  * @param manager        environment manager that used by executor
  * @param batchCollector implementation of [[BatchCollector]] that used with executor
  * @tparam T type of incoming data
  * @author Pavel Tomskikh
  */
class BatchEngineSimulator[T <: AnyRef](executor: BatchStreamingExecutor[T],
                                        manager: ModuleEnvironmentManagerMock,
                                        batchCollector: BatchCollector)
  extends CommonEngineSimulator[T](executor, manager) {

  /**
    * Sends incoming envelopes from local buffer to [[executor]] as long as simulator can creates windows and returns
    * output elements, state and envelopes that does not processed
    *
    * @param batchesNumberBeforeIdle  number of retrieved batches between invocations of [[executor.onIdle()]].
    *                                 '0' means that [[executor.onIdle()]] will never be called.
    * @param window                   count of batches that will be contained into a window ([[BatchInstance.window]])
    * @param slidingInterval          the interval at which a window will be shifted (count of processed batches that will be
    *                                 removed from the window)
    * @param removeProcessedEnvelopes indicates that processed envelopes must be removed from local buffer
    * @return output elements, state and envelopes that haven't been processed
    */
  def process(batchesNumberBeforeIdle: Int = 0,
              window: Int,
              slidingInterval: Int,
              removeProcessedEnvelopes: Boolean = true): BatchSimulationResult = {

    val remainingEnvelopes = new EnvelopesProcessor(batchesNumberBeforeIdle, window, slidingInterval).process()

    if (removeProcessedEnvelopes) {
      clear()
      inputEnvelopes ++= remainingEnvelopes
    }

    BatchSimulationResult(simulationResult, remainingEnvelopes)
  }

  private class EnvelopesProcessor(batchesNumberBeforeIdle: Int = 0,
                                   window: Int,
                                   slidingInterval: Int) {

    require(window > 0, "'window' must be positive")
    require(
      slidingInterval > 0 && slidingInterval <= window,
      "'slidingInterval' must be positive and less or equal than 'window'")

    private val inputs: Array[String] = inputEnvelopes.map(_.stream).toSet.toArray
    private var batchesAfterIdle: Int = 0
    private val envelopesByStream: Map[String, mutable.Queue[Envelope]] =
      inputs.map(stream => stream -> mutable.Queue.empty[Envelope]).toMap
    private var retrievableStreams: Array[String] = inputs
    private val windowRepository: WindowRepository = createWindowRepository
    private val currentWindowPerStream: mutable.Map[String, Window] = mutable.Map(inputs.map(x => (x, new Window(x))): _*)
    private val counterOfBatchesPerStream: mutable.Map[String, Int] = mutable.Map(inputs.map(x => (x, 0)): _*)
    private val collectedWindowPerStream: mutable.Map[String, Window] = mutable.Map.empty

    def process(): Seq[Envelope] = {
      inputEnvelopes.foreach(envelope => envelopesByStream(envelope.stream).enqueue(envelope))
      var canContinue: Boolean = envelopesByStream.forall(_._2.length >= window)

      while (canContinue) {
        retrievableStreams.foreach { stream =>
          envelopesByStream(stream).dequeueFirst(_ => true) match {
            case Some(envelope) =>
              batchCollector.onReceive(envelope)
              processBatches()

              if (inputs.forall(collectedWindowPerStream.isDefinedAt)) {
                onWindow()
                canContinue = envelopesByStream.forall(_._2.length >= slidingInterval)
              }

            case None =>
          }
        }
      }

      envelopesByStream.flatMap(_._2).toSeq
    }


    private def processBatches(): Unit = {
      val batches = batchCollector.getBatchesToCollect().map(batchCollector.collectBatch)

      batches.foreach { batch =>
        registerBatch(batch)

        if (counterOfBatchesPerStream(batch.stream) == window) {
          collectWindow(batch.stream)
          retrievableStreams = retrievableStreams.filter(_ != batch.stream)
        }

        if (batchesNumberBeforeIdle > 0) {
          batchesAfterIdle += 1
          if (batchesAfterIdle == batchesNumberBeforeIdle) {
            executor.onIdle()
            batchesAfterIdle = 0
          }
        }
      }
    }

    private def registerBatch(batch: Batch): Unit = {
      currentWindowPerStream(batch.stream).batches += batch
      counterOfBatchesPerStream(batch.stream) += 1
    }

    private def collectWindow(stream: String): Unit = {
      val collectedWindow = currentWindowPerStream(stream)
      collectedWindowPerStream(stream) = collectedWindow.copy()
      currentWindowPerStream(stream).batches.remove(0, slidingInterval)
      counterOfBatchesPerStream(stream) -= slidingInterval
    }

    private def onWindow(): Unit = {
      prepareCollectedWindows()
      executor.onWindow(windowRepository)
      retrievableStreams = inputs
    }

    private def prepareCollectedWindows(): Unit = {
      collectedWindowPerStream.foreach {
        case (s, w) =>
          windowRepository.put(s, w)
      }
      collectedWindowPerStream.clear()
    }


    private def createWindowRepository: WindowRepository = {
      val instance: BatchInstance = {
        val instanceMock = mock(classOf[BatchInstance])
        when(instanceMock.window).thenReturn(window)
        when(instanceMock.slidingInterval).thenReturn(slidingInterval)
        when(instanceMock.getInputsWithoutStreamMode).thenReturn(inputs)
        instanceMock
      }

      new WindowRepository(instance)
    }
  }

}

/**
  * Contains output elements, state and list of envelopes
  *
  * @param simulationResult   output elements and state
  * @param remainingEnvelopes list of envelopes that haven't been processed
  */
case class BatchSimulationResult(simulationResult: SimulationResult, remainingEnvelopes: Seq[Envelope])
