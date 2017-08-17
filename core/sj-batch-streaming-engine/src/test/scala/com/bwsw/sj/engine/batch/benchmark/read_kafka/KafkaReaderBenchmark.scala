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
package com.bwsw.sj.engine.batch.benchmark.read_kafka

import java.io.File
import java.util.UUID

import com.bwsw.common.KafkaClient
import com.bwsw.sj.common.utils.benchmark.BenchmarkUtils.retrieveResultFromFile
import com.bwsw.sj.common.utils.benchmark.KafkaDataSender

/**
  * Provides methods for testing the speed of reading data from Kafka by some application in a batch mode.
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
  protected val kafkaSender: KafkaDataSender = new KafkaDataSender(kafkaAddress, kafkaTopic, words, " ")

  protected val warmingUpMessageSize: Long = 10
  protected val warmingUpMessagesCount: Long = 10
  protected val warmingUpBatchSize: Int = 100
  protected val warmingUpWindowSize: Int = 1
  protected val warmingUpSlidingInterval: Int = 1

  protected val lookupResultTimeout: Long = 5000
  protected val outputFilename = "benchmark-output-" + UUID.randomUUID().toString
  protected val outputFile = new File(outputFilename)

  /**
    * Performs the first test because it needs more time than subsequent tests
    */
  def warmUp(): Long = runTest(
    warmingUpMessageSize,
    warmingUpMessagesCount,
    warmingUpBatchSize,
    warmingUpWindowSize,
    warmingUpSlidingInterval)

  /**
    * Sends data into the Kafka server and runs an application under test
    *
    * @param messageSize     size of one message that is sent to the Kafka server
    * @param messagesCount   count of messages
    * @param batchSize       size of batch in milliseconds
    * @param windowSize      count of batches that will be contained into a window
    * @param slidingInterval the interval at which a window will be shifted (count of processed batches that will be removed
    *                        from the window)
    * @return time in milliseconds within which an application under test reads messages from Kafka
    */
  def runTest(messageSize: Long,
              messagesCount: Long,
              batchSize: Int,
              windowSize: Int,
              slidingInterval: Int): Long = {
    println(s"Messages count: $messagesCount")
    println(s"Message size: $messageSize")
    println(s"Batch size: $batchSize")
    println(s"Window size: $windowSize")
    println(s"Sliding interval: $slidingInterval")

    kafkaClient.createTopic(kafkaTopic, 1, 1)
    while (!kafkaClient.topicExists(kafkaTopic))
      Thread.sleep(100)

    println(s"Kafka topic $kafkaTopic created")

    kafkaSender.send(messageSize, messagesCount)
    println("Data sent to the Kafka")

    val process: Process = runProcess(
      messageSize,
      messagesCount,
      batchSize,
      windowSize,
      slidingInterval)

    var maybeResult: Option[Long] = retrieveResultFromFile(outputFile).map(_.toLong)
    while (maybeResult.isEmpty) {
      Thread.sleep(lookupResultTimeout)
      maybeResult = retrieveResultFromFile(outputFile).map(_.toLong)
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
  protected def runProcess(messageSize: Long,
                           messagesCount: Long,
                           batchSize: Int,
                           windowSize: Int,
                           slidingInterval: Int): Process
}
