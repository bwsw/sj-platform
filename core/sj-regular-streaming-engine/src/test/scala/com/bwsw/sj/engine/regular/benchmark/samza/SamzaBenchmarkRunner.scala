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
package com.bwsw.sj.engine.regular.benchmark.samza

import java.io.{File, FileWriter}
import java.util.Calendar

import com.bwsw.sj.common.utils.BenchmarkConfigNames._
import com.bwsw.sj.common.utils.BenchmarkLiterals.samzaDefaultOutputFile
import com.typesafe.config.ConfigFactory

import scala.util.Try

/**
  * Performs [[SamzaBenchmark]]
  *
  * Configuration:
  *
  * sj-benchmark.performance.message.sizes - list of messages' sizes that separated by a comma (',').
  * Environment variable MESSAGE_SIZES.
  * sj-benchmark.performance.message.count - count of messages per test (10000000 by default). Environment variable MESSAGES_COUNT.
  * sj-benchmark.performance.kafka.address - Kafka server's address. Environment variable KAFKA_ADDRESS.
  * sj-benchmark.performance.zookeeper.address - ZooKeeper server's address. Must point to the ZooKeeper server that used
  * by the Kafka server. Environment variable ZOOKEEPER_ADDRESS.
  * sj-benchmark.performance.output-file - file to output results in csv format (message size, milliseconds)
  * (samza-benchmark-output by default). Environment variable OUTPUT_FILE.
  * sj-benchmark.performance.words = List of words that sends to the Kafka server ("lorem,ipsum,dolor,sit,amet" by default).
  * Environment variable WORDS.
  *
  * @author Pavel Tomskikh
  */
object SamzaBenchmarkRunner extends App {
  println(Calendar.getInstance().getTime)

  private val config = ConfigFactory.load()
  private val zooKeeperAddress = config.getString(zooKeeperAddressConfig)
  private val kafkaAddress = config.getString(kafkaAddressConfig)
  private val messagesCount = config.getLong(messagesCountConfig)
  private val words = config.getString(wordsConfig).split(",")
  private val outputFileName = Try(config.getString(outputFileConfig)).getOrElse(samzaDefaultOutputFile)
  private val messageSizes = config.getString(messageSizesConfig).split(",").map(_.toLong)

  private val samzaBenchmark = new SamzaBenchmark(zooKeeperAddress, kafkaAddress, messagesCount, words)

  private val results = messageSizes.map { messageSize =>
    (messageSize, samzaBenchmark.runTest(messageSize))
  }

  samzaBenchmark.close()

  println("DONE")
  println("Result:")
  println(results.mkString("\n"))

  private val writer = new FileWriter(new File(outputFileName))
  writer.write(results.map { case (messageSize, time) => s"$messageSize,$time" }.mkString("\n"))
  writer.close()

  println(Calendar.getInstance().getTime)
}
