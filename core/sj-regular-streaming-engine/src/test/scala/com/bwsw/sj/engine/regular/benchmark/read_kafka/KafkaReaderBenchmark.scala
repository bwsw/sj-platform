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
package com.bwsw.sj.engine.regular.benchmark.read_kafka

import java.util.UUID

import com.bwsw.common.KafkaClient
import com.bwsw.sj.kafka.data_sender.DataSender

/**
  * Provides methods for testing the speed of reading data from Kafka by some application.
  *
  * Topic deletion must be enabled on the Kafka server.
  *
  * @param zooKeeperAddress ZooKeeper server's address. Must point to the ZooKeeper server that used by the Kafka server.
  * @param kafkaAddress     Kafka server's address
  * @param words            list of words that are sent to the kafka server
  * @author Pavel Tomskikh
  */
abstract class KafkaReaderBenchmark(zooKeeperAddress: String,
                                    kafkaAddress: String,
                                    words: Array[String]) {
  protected val kafkaTopic: String = "performance-benchmark-" + UUID.randomUUID().toString
  protected val kafkaClient: KafkaClient = new KafkaClient(Array(zooKeeperAddress))
  protected val kafkaSender: DataSender = new DataSender(kafkaAddress, kafkaTopic, words, " ")
  protected val warmingUpMessageSize: Long = 10
  protected val warmingUpMessagesCount: Long = 10
  protected val lookupResultTimeout: Long = 5000

  /**
    * Performs the first test because it needs more time than subsequent tests
    */
  def warmUp(): Long = runTest(warmingUpMessageSize, warmingUpMessagesCount)

  /**
    * Sends data into the Kafka server and runs an application under test
    *
    * @param messageSize   size of one message that is sent to the Kafka server
    * @param messagesCount count of messages
    * @return time in milliseconds within which an application under test reads messages from Kafka
    */
  def runTest(messageSize: Long, messagesCount: Long): Long = {
    println(s"$messagesCount messages of $messageSize bytes")

    kafkaClient.createTopic(kafkaTopic, 1, 1)
    while (!kafkaClient.topicExists(kafkaTopic))
      Thread.sleep(100)

    println(s"Kafka topic $kafkaTopic created")

    kafkaSender.send(messageSize, messagesCount, firstMessage(messageSize, messagesCount))
    println("Data sent to the Kafka")

    val process: Process = runProcess(messageSize, messagesCount)

    var maybeResult: Option[Long] = retrieveResult(messageSize, messagesCount)
    while (maybeResult.isEmpty) {
      Thread.sleep(lookupResultTimeout)
      maybeResult = retrieveResult(messageSize, messagesCount)
    }

    process.destroy()

    kafkaClient.deleteTopic(kafkaTopic)
    while (kafkaClient.topicExists(kafkaTopic))
      Thread.sleep(100)

    println(s"Kafka topic $kafkaTopic deleted")

    maybeResult.get
  }

  /**
    * Closes opened connections, deletes temporary files
    */
  def close(): Unit =
    kafkaClient.close()


  /**
    * Used to run an application under test in a separate process
    *
    * @param messageSize   size of one message that is sent to the Kafka server
    * @param messagesCount count of messages
    * @return process of an application under test
    */
  protected def runProcess(messageSize: Long, messagesCount: Long): Process

  /**
    * Is invoked every [[lookupResultTimeout]] milliseconds until result retrieved.
    * A result is a time in milliseconds within which an application under test reads messages from Kafka.
    *
    * @param messageSize   size of one message that is sent to the Kafka server
    * @param messagesCount count of messages
    * @return result if a test has done or None otherwise
    */
  protected def retrieveResult(messageSize: Long, messagesCount: Long): Option[Long]

  /**
    * Used to create a specific message that will be sent to the Kafka server as the first message
    *
    * @param messageSize   size of one message that is sent to the Kafka server
    * @param messagesCount count of messages
    * @return specific first message or None
    */
  protected def firstMessage(messageSize: Long, messagesCount: Long): Option[String] = None
}
