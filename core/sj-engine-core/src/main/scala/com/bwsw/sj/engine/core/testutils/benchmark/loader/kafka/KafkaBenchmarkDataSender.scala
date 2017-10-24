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
import com.bwsw.sj.engine.core.testutils.benchmark.loader.BenchmarkDataSender

/**
  * Provides methods for sending data into Kafka topic for test some application
  *
  * @author Pavel Tomskikh
  */
class KafkaBenchmarkDataSender(config: KafkaBenchmarkDataLoaderConfig)
  extends BenchmarkDataSender[KafkaBenchmarkDataSenderParameters] {

  private val client: KafkaClient = new KafkaClient(Array(config.zooKeeperAddress))
  private val sender = new KafkaDataSender(config.kafkaAddress, config.topic, config.words, " ")

  /**
    * Sends data into Kafka topic for first test
    */
  override def warmUp(): Unit = {
    clearStorage()
    sender.send(warmingUpMessageSize, warmingUpMessagesCount)
  }

  /**
    * Removes data from Kafka topic
    */
  override def clearStorage(): Unit = {
    deleteTopic()
    createTopic()
  }

  override def iterator: Iterator[KafkaBenchmarkDataSenderParameters] = {
    new Iterator[KafkaBenchmarkDataSenderParameters] {

      private val messageSizeIterator = config.messageSizes.iterator
      private var messagesCountIterator = config.messagesCounts.iterator
      private var messageSize = messageSizeIterator.next()
      private var currentTopicSize: Long = 0

      override def hasNext: Boolean =
        messageSizeIterator.hasNext || messagesCountIterator.hasNext

      override def next(): KafkaBenchmarkDataSenderParameters = {
        if (messagesCountIterator.isEmpty) {
          messageSize = messageSizeIterator.next()
          messagesCountIterator = config.messagesCounts.iterator
          clearStorage()
          currentTopicSize = 0
        }

        val messagesCount = messagesCountIterator.next()
        if (messagesCount > currentTopicSize) {
          val appendedMessages = messagesCount - currentTopicSize
          sender.send(messageSize, appendedMessages + appendedMessages / 10)
          currentTopicSize = messagesCount
        }

        KafkaBenchmarkDataSenderParameters(messageSize, messagesCount)
      }
    }
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
