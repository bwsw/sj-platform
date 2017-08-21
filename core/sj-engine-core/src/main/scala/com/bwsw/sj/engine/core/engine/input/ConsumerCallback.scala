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
package com.bwsw.sj.engine.core.engine.input

import com.bwsw.common.SerializerInterface
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.engine.core.entities.{EnvelopeInterface, WeightedBlockingQueue, TStreamEnvelope}
import com.bwsw.tstreams.agents.consumer.subscriber.Callback
import com.bwsw.tstreams.agents.consumer.{Consumer, ConsumerTransaction, TransactionOperator}
import org.slf4j.LoggerFactory
import scaldi.Injectable.inject
import scaldi.Injector

/**
  * Provides a handler for sub. consumer that puts a t-stream envelope in a persistent blocking queue
  *
  * @author Kseniya Mikhaleva
  * @param blockingQueue Persistent blocking queue for storing transactions
  */

class ConsumerCallback[T <: AnyRef](envelopeDataSerializer: SerializerInterface,
                                    blockingQueue: WeightedBlockingQueue[EnvelopeInterface])
                                   (implicit injector: Injector) extends Callback {
  private val logger = LoggerFactory.getLogger(this.getClass)

  override def onTransaction(operator: TransactionOperator, transaction: ConsumerTransaction): Unit = {
    val consumer = operator.asInstanceOf[Consumer]
    logger.debug(s"onTransaction handler was invoked by subscriber: ${consumer.name}.")
    val stream = inject[ConnectionRepository].getStreamRepository.get(consumer.stream.name).get

    val data = transaction.getAll.map(envelopeDataSerializer.deserialize(_).asInstanceOf[T])
    val envelope = new TStreamEnvelope(data, consumer.name)
    envelope.stream = stream.name
    envelope.partition = transaction.getPartition
    envelope.tags = stream.tags
    envelope.id = transaction.getTransactionID

    blockingQueue.put(envelope)
  }
}
