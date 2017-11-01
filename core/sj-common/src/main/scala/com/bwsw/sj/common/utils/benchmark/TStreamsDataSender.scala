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
package com.bwsw.sj.common.utils.benchmark

import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.tstreams.agents.producer.NewProducerTransactionPolicy
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}

import scala.util.Random

/**
  * Choose random words, concatenates them, and sends to a T-Streams server
  *
  * @param address    address a ZooKeeper server
  * @param streamName name of stream
  * @param token      authentication token
  * @param prefix     ZooKeeper root node which holds coordination tree
  * @param words      available words
  * @param separator  separator between words
  * @author Pavel Tomskikh
  */
class TStreamsDataSender(address: String,
                         streamName: String,
                         token: String,
                         prefix: String,
                         words: Seq[String],
                         separator: String) {

  private val tStreamsFactory = new TStreamsFactory
  tStreamsFactory.setProperty(ConfigurationOptions.Coordination.endpoints, address)
  tStreamsFactory.setProperty(ConfigurationOptions.Common.authenticationKey, token)
  tStreamsFactory.setProperty(ConfigurationOptions.Stream.name, streamName)
  tStreamsFactory.setProperty(ConfigurationOptions.Stream.partitionsCount, 1)
  tStreamsFactory.setProperty(ConfigurationOptions.Coordination.path, prefix)

  /**
    * Generates data and send it to a tstream server
    *
    * @param messageSize        size of one message
    * @param messages           count of messages
    * @param sizePerTransaction count of messages per transaction
    */
  def send(messageSize: Long, messages: Long, sizePerTransaction: Long): Unit = {
    val client = tStreamsFactory.getStorageClient()
    if (!client.checkStreamExists(streamName)) {
      client.createStream(streamName, 1, StreamLiterals.ttl, "")
    }

    val producer = tStreamsFactory.getProducer("producer_for_benchmark", Set(0))
    var transaction = producer.newTransaction(NewProducerTransactionPolicy.CheckpointIfOpened, 0)

    (1l to messages).foreach { i =>
      var message = words(Random.nextInt(words.length))
      while (message.getBytes.length < messageSize)
        message += separator + words(Random.nextInt(words.length))

      transaction.send(message)

      if (i % sizePerTransaction == 0) {
        transaction.checkpoint()
        transaction = producer.newTransaction(NewProducerTransactionPolicy.CheckpointIfOpened, 0)
      }
    }

    transaction.checkpoint()

    producer.stop()
    producer.close()
  }

  def close(): Unit = {
    tStreamsFactory.close()
  }
}
