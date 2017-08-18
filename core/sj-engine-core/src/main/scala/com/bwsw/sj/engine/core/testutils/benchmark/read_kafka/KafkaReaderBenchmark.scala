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
package com.bwsw.sj.engine.core.testutils.benchmark.read_kafka

import java.io.{BufferedReader, File, FileReader}
import java.util.UUID

import com.bwsw.common.KafkaClient
import com.bwsw.sj.common.utils.benchmark.KafkaDataSender

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
  protected val kafkaSender: KafkaDataSender = new KafkaDataSender(kafkaAddress, kafkaTopic, words, " ")

  protected val warmingUpMessageSize: Long = 10
  protected val warmingUpMessagesCount: Long = 10

  protected val lookupResultTimeout: Long = 5000
  protected val outputFilename = "benchmark-output-" + UUID.randomUUID().toString
  protected val outputFile = new File(outputFilename)

  /**
    * Performs the first test because it needs more time than subsequent tests
    */
  def warmUp(): Long

  /**
    * Closes opened connections, deletes temporary files
    */
  def close(): Unit =
    kafkaClient.close()

  /**
    * Retrieves result from file
    *
    * @return result if a file exists or None otherwise
    */
  protected def retrieveResultFromFile(): Option[Long] = {
    if (outputFile.exists()) {
      val reader = new BufferedReader(new FileReader(outputFile))
      val result = reader.readLine()
      reader.close()
      outputFile.delete()

      Some(result.toLong)
    }
    else
      None
  }
}
