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
package com.bwsw.sj.engine.core.simulation.state

import com.bwsw.sj.common.engine.core.entities.{Envelope, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.engine.{CheckpointHandlers, StateHandlers, StreamingExecutor, TimerHandlers}
import com.bwsw.sj.engine.core.simulation.SimulatorConstants.defaultConsumerName

import scala.collection.mutable

/**
  * Provides methods for [[com.bwsw.sj.engine.core.simulation.regular.RegularEngineSimulator]] and
  * [[com.bwsw.sj.engine.core.simulation.batch.BatchEngineSimulator]]
  *
  * @author Pavel Tomskikh
  */
class CommonEngineSimulator[T <: AnyRef](executor: StreamingExecutor with StateHandlers with CheckpointHandlers with TimerHandlers,
                                         manager: ModuleEnvironmentManagerMock) {

  protected var transactionID: Long = 0
  protected val inputEnvelopes: mutable.Buffer[Envelope] = mutable.Buffer.empty

  /**
    * Load a state in a state storage
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
    * Creates [[KafkaEnvelope]]s and saves it in a local buffer.
    * For each element from list creates a new [[KafkaEnvelope]].
    *
    * @param entities incoming data
    * @param stream   name of stream
    * @return IDs of saved transactions
    */
  def prepareKafka(entities: Seq[T], stream: String): Seq[Long] =
    entities.map(prepareKafka(_, stream))

  /**
    * Simulates behavior of [[com.bwsw.sj.common.engine.TaskEngine TaskEngine]] before checkpoint
    *
    * @param isFullState flag denotes that the full state(true) or partial changes of state(false) is going to be saved
    * @return output elements and state
    */
  def beforeCheckpoint(isFullState: Boolean): SimulationResult = {
    executor.onBeforeCheckpoint()
    executor.onBeforeStateSave(isFullState)

    val result = simulationResult
    manager.producerPolicyByOutput.values.foreach {
      case (_, moduleOutput: ModuleOutputMockHelper) =>
        moduleOutput.clear()
      case _ =>
        throw new IllegalStateException("Incorrect outputs")
    }

    result
  }

  /**
    * Invokes [[executor.onTimer(jitter)]]
    *
    * @param jitter Delay between a real response time and an invocation of this handler
    * @return output elements and state
    */
  def timer(jitter: Long): SimulationResult = {
    executor.onTimer(jitter)

    simulationResult
  }

  /**
    * Removes all envelopes from a local buffer
    */
  def clear(): Unit =
    inputEnvelopes.clear()


  protected def simulationResult: SimulationResult = {
    val streamData = manager.producerPolicyByOutput.map {
      case (stream, (_, moduleOutput: ModuleOutputMockHelper)) =>
        StreamData(stream, moduleOutput.getPartitionDataList)
      case _ =>
        throw new IllegalStateException("Incorrect outputs")
    }.toSeq

    SimulationResult(streamData, manager.getState.getAll)
  }
}
