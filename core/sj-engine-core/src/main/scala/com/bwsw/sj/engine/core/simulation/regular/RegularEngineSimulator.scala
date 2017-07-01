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

import com.bwsw.sj.common.engine.core.entities.{KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.common.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.core.simulation.SimulatorConstants.defaultConsumerName

import scala.collection.mutable

/**
  * SImulates behavior of [[com.bwsw.sj.common.engine.TaskEngine TaskEngine]] for testing of
  * [[RegularStreamingExecutor]]
  *
  * @param executor implementation of [[RegularStreamingExecutor]] under test
  * @param manager  environment manager that used by executor
  * @tparam T type of incoming data
  * @author Pavel Tomskikh
  */
class RegularEngineSimulator[T <: AnyRef](executor: RegularStreamingExecutor[T],
                                          manager: ModuleEnvironmentManager) {

  private var transactionID: Long = 0
  private val tStreamEnvelopes: mutable.Buffer[TStreamEnvelope[T]] = mutable.Buffer.empty
  private val kafkaEnvelopes: mutable.Buffer[KafkaEnvelope[T]] = mutable.Buffer.empty

  executor.onInit()

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

    tStreamEnvelopes += envelope
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

    kafkaEnvelopes += envelope
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
}
