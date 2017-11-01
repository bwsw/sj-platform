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
package com.bwsw.sj.engine.core.testutils.benchmark.loader.kafka

import com.bwsw.common.KafkaClient
import com.bwsw.sj.common.utils.benchmark.KafkaDataSender
import com.bwsw.sj.engine.core.testutils.benchmark.loader.{BenchmarkDataSender, SenderFactory}
import com.typesafe.config.Config

/**
  * Provides methods for sending data into Kafka topic for test some application
  *
  * @author Pavel Tomskikh
  */
class KafkaBenchmarkDataSender(config: KafkaBenchmarkDataSenderConfig)
  extends BenchmarkDataSender[KafkaBenchmarkDataSenderParameters] {

  override val warmingUpParameters = KafkaBenchmarkDataSenderParameters(10, 10)
  private val client: KafkaClient = new KafkaClient(Array(config.zooKeeperAddress))
  private val sender = new KafkaDataSender(config.kafkaAddress, config.topic, config.words, " ")
  private var currentMessageSize: Long = 0
  private var currentStorageSize: Long = 0

  clearStorage()

  /**
    * Removes data from Kafka topic
    */
  override def clearStorage(): Unit = {
    deleteTopic()
    createTopic()
  }

  /**
    * Sends data into Kafka topic
    *
    * @param parameters sending data parameters
    */
  override def send(parameters: KafkaBenchmarkDataSenderParameters): Unit = {
    if (parameters.messageSize != currentMessageSize) {
      clearStorage()
      currentStorageSize = 0
      currentMessageSize = parameters.messageSize
    }

    if (parameters.messagesCount > currentStorageSize) {
      val appendedMessages = parameters.messagesCount - currentStorageSize
      sender.send(currentMessageSize, appendedMessages + appendedMessages / 10)
      currentStorageSize = parameters.messagesCount
    }
  }

  override def iterator: Iterator[KafkaBenchmarkDataSenderParameters] = {
    config.messageSizes.flatMap { messageSize =>
      config.messagesCounts.map { messagesCount =>
        KafkaBenchmarkDataSenderParameters(messageSize, messagesCount)
      }
    }.iterator
  }

  /**
    * Closes connection with Kafka
    */
  override def stop(): Unit =
    client.close()

  /**
    * Creates topic if it does not exists
    */
  private def createTopic(): Unit = {
    if (!client.topicExists(config.topic)) {
      client.createTopic(config.topic, 1, 1)
      while (!client.topicExists(config.topic))
        Thread.sleep(100)
    }
  }

  /**
    * Deletes topic if it exists
    */
  private def deleteTopic(): Unit = {
    if (client.topicExists(config.topic)) {
      client.deleteTopic(config.topic)
      while (client.topicExists(config.topic))
        Thread.sleep(100)
    }
  }
}

object KafkaBenchmarkDataSender
  extends SenderFactory[KafkaBenchmarkDataSenderParameters, KafkaBenchmarkDataSenderConfig] {

  override def create(config: Config): (KafkaBenchmarkDataSender, KafkaBenchmarkDataSenderConfig) = {
    val senderConfig = new KafkaBenchmarkDataSenderConfig(config)
    val sender = new KafkaBenchmarkDataSender(senderConfig)

    (sender, senderConfig)
  }
}
