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
package com.bwsw.sj.engine.core.testutils.benchmark.read_kafka.regular

import com.bwsw.sj.engine.core.testutils.benchmark.read_kafka.KafkaReaderBenchmark

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
abstract class RegularKafkaReaderBenchmark(zooKeeperAddress: String,
                                           kafkaAddress: String,
                                           words: Array[String])
  extends KafkaReaderBenchmark(zooKeeperAddress, kafkaAddress, words) {

  /**
    * Performs the first test because it needs more time than subsequent tests
    */
  def warmUp(): Long = {
    sendData(warmingUpMessageSize, warmingUpMessagesCount)
    runTest(warmingUpMessagesCount)
  }

  /**
    * Runs an application under test
    *
    * @param messagesCount count of messages
    * @return time in milliseconds within which an application under test reads messages from Kafka
    */
  def runTest(messagesCount: Long): Long = {
    val process: Process = runProcess(messagesCount)

    var maybeResult: Option[Long] = retrieveResultFromFile()
    while (maybeResult.isEmpty && process.isAlive) {
      Thread.sleep(lookupResultTimeout)
      maybeResult = retrieveResultFromFile()
    }

    if (process.isAlive)
      process.destroy()

    maybeResult.getOrElse(-1L)
  }


  /**
    * Used to run an application under test in a separate process
    *
    * @param messagesCount count of messages
    * @return process of an application under test
    */
  protected def runProcess(messagesCount: Long): Process
}
