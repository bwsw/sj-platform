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
    * @param windowsNumberBeforeIdle  number of windows between invocations of [[executor.onIdle()]]. '0' means that
    *                                 [[executor.onIdle()]] will never be called.
    * @param window                   count of batches that will be contained into a window ([[BatchInstance.window]])
    * @param slidingInterval          the interval at which a window will be shifted (сount of batches that will be
    *                                 removed from the window after its processing)
    * @param removeProcessedEnvelopes indicates that processed envelopes must be removed from local buffer
    * @return output elements, state and envelopes that does not processed
    */
  def process(windowsNumberBeforeIdle: Int = 0,
              window: Int,
              slidingInterval: Int,
              removeProcessedEnvelopes: Boolean = true) = {

    val remainingEnvelopes = new Processor(windowsNumberBeforeIdle, window, slidingInterval).process()

    if (removeProcessedEnvelopes) {
      clear()
      inputEnvelopes ++= remainingEnvelopes
    }

    BatchSimulationResult(simulationResult, remainingEnvelopes)
  }

  private class Processor(windowsNumberBeforeIdle: Int = 0,
                          window: Int,
                          slidingInterval: Int) {

    private val inputs: Array[String] = inputEnvelopes.map(_.stream).toSet.toArray
    private var windowsAfterIdle: Int = 0
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

      if (windowsNumberBeforeIdle > 0) {
        windowsAfterIdle += 1
        if (windowsAfterIdle == windowsNumberBeforeIdle) {
          executor.onIdle()
          windowsAfterIdle = 0
        }
      }
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
  * @param remainingEnvelopes list of envelopes
  */
case class BatchSimulationResult(simulationResult: SimulationResult, remainingEnvelopes: Seq[Envelope])
