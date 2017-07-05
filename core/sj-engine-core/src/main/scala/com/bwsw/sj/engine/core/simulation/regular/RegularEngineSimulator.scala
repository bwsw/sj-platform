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
package com.bwsw.sj.engine.core.simulation.regular

import com.bwsw.sj.common.engine.core.entities.{Envelope, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.core.simulation.SimulatorConstants.defaultConsumerName
import com.bwsw.sj.engine.core.simulation.state.{ModuleEnvironmentManagerMock, ModuleOutputMockHelper, OutputElement}

import scala.collection.mutable

/**
  * Simulates behavior of [[com.bwsw.sj.common.engine.TaskEngine TaskEngine]] for testing an implementation of
  * [[RegularStreamingExecutor]]
  *
  * Usage example:
  * {{{
  * val stateSaver = mock(classOf[StateSaverInterface])
  * val stateLoader = new StateLoaderMock
  * val stateService = new RAMStateService(stateSaver, stateLoader)
  * val stateStorage = new StateStorage(stateService)
  * val options = ""
  * val output = new TStreamStreamDomain("out", mock(classOf[TStreamServiceDomain]), 1, tags = Array("output"))
  * val manager = new ModuleEnvironmentManagerMock(stateStorage, options, Array(output))
  * val executor: RegularStreamingExecutor[String] = new MyExecutor(manager)
  * val checkpointInterval = 3
  * val tstreamInput = "t-stream-input"
  * val kafkaInput = "kafka-input"
  *
  * val simulator = new RegularEngineSimulator(executor, manager, checkpointInterval)
  * simulator.prepareState(Map("var1" -> 1, "var2" -> 2))
  * simulator.prepareTstream(Seq("a", "b", "c"), tstreamInput)
  * simulator.prepareKafka(Seq("d", "e"), kafkaInput)
  * val results = simulator.process()
  * println(results)
  * }}}
  *
  * @param executor           implementation of [[RegularStreamingExecutor]] under test
  * @param manager            environment manager that used by executor
  * @param checkpointInterval number of envelopes before between checkpoints
  * @tparam T type of incoming data
  * @author Pavel Tomskikh
  */
class RegularEngineSimulator[T <: AnyRef](executor: RegularStreamingExecutor[T],
                                          manager: ModuleEnvironmentManagerMock,
                                          checkpointInterval: Long) {

  private var transactionID: Long = 0
  private val inputEnvelopes: mutable.Buffer[Envelope] = mutable.Buffer.empty
  private var envelopesWithoutCheckpoint: Long = 0

  /**
    * Load state in state storage
    *
    * @param state key/value map
    */
  def prepareState(state: Map[String, Any]): Unit = state.foreach {
    case (key, value) =>
      manager.getState.set(key, value)
  }

  /**
    * Creates [[TStreamEnvelope]] and saves it in a local buffer.
    *
    * @param entities     incoming data
    * @param stream       name of stream
    * @param consumerName name of consumer
    * @return ID of saved transaction
    */
  def prepareTstream(entities: Seq[T], stream: String, consumerName: String = defaultConsumerName): Long = {
    val queue = mutable.Queue(entities: _*)
    val envelope = new TStreamEnvelope[T](queue, consumerName)
    transactionID += 1
    envelope.id = transactionID
    envelope.stream = stream

    inputEnvelopes += envelope
    transactionID
  }

  /**
    * Creates [[KafkaEnvelope]] and saves it in a local buffer
    *
    * @param entity incoming data
    * @param stream name of stream
    * @return ID of saved transaction
    */
  def prepareKafka(entity: T, stream: String): Long = {
    val envelope = new KafkaEnvelope[T](entity)
    transactionID += 1
    envelope.id = transactionID
    envelope.stream = stream

    inputEnvelopes += envelope
    transactionID
  }

  /**
    * Creates [[KafkaEnvelope]]s and saves it in a local buffer. For each element from entities creates one
    * [[KafkaEnvelope]].
    *
    * @param entities incoming data
    * @param stream   name of stream
    * @return IDs of saved transactions
    */
  def prepareKafka(entities: Seq[T], stream: String): Seq[Long] =
    entities.map(prepareKafka(_, stream))

  /**
    * Sends all incoming envelopes from local buffer to [[executor]] and builds simulation results
    *
    * @param clearBuffer indicates that local buffer must be cleared
    * @return collection of simulation results
    */
  def process(clearBuffer: Boolean = true): mutable.Buffer[SimulationResult] = {
    val results = inputEnvelopes.map { envelope =>
      envelope match {
        case tStreamEnvelope: TStreamEnvelope[T] =>
          executor.onMessage(tStreamEnvelope)
        case kafkaEnvelope: KafkaEnvelope[T] =>
          executor.onMessage(kafkaEnvelope)
      }
      val state = manager.getState.getAll

      if (envelopesWithoutCheckpoint == checkpointInterval || manager.isCheckpointInitiated) {
        executor.onBeforeCheckpoint()
        executor.onAfterCheckpoint()
        envelopesWithoutCheckpoint = 0
      } else
        envelopesWithoutCheckpoint += 1

      val outputElements = manager.producerPolicyByOutput.mapValues {
        case (_, moduleOutput: ModuleOutputMockHelper) =>
          moduleOutput.readOutputElements()
        case _ =>
          throw new IllegalStateException("Incorrect outputs")
      }.filter(_._2.nonEmpty)

      SimulationResult(envelope.id, outputElements, state)
    }

    if (clearBuffer) clear()

    results
  }

  /**
    * Removes all envelopes from local buffer
    */
  def clear(): Unit =
    inputEnvelopes.clear()
}

/**
  * Contains results of simulation
  *
  * @param transactionId  ID of incoming transaction
  * @param outputElements outgoing entities
  */
case class SimulationResult(transactionId: Long,
                            outputElements: collection.Map[String, mutable.Buffer[OutputElement]],
                            state: Map[String, Any])
