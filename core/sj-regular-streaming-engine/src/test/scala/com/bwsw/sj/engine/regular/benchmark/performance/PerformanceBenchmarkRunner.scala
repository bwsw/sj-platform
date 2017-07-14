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
package com.bwsw.sj.engine.regular.benchmark.performance

import java.util.Calendar

import com.bwsw.sj.common.SjModule
import com.bwsw.sj.common.engine.core.config.EngineConfigNames
import com.bwsw.sj.common.utils.BenchmarkConfigNames._
import com.bwsw.sj.common.utils.CommonAppConfigNames.{zooKeeperHost, zooKeeperPort}
import com.typesafe.config.ConfigFactory

/**
  * Performs [[PerformanceBenchmark]].
  *
  * Configuration:
  *
  * sj-benchmark.performance.message.sizes - list of messages' sizes that separated by a comma (',').
  * Environment variable MESSAGE_SIZES.
  * sj-benchmark.performance.message.count - count of messages per test (10000000 by default). Environment variable MESSAGES_COUNT.
  * sj-benchmark.performance.kafka.address - Kafka server's address. Environment variable KAFKA_ADDRESS.
  * sj-benchmark.performance.mongo.port - Port for embedded mongo server. Environment variable MONGO_PORT.
  * sj-benchmark.performance.output-file - file to output results in csv format (message size, milliseconds)
  * (benchmark-output-file by default). Environment variable OUTPUT_FILE.
  * sj-benchmark.performance.words = List of words that sends to the Kafka server ("lorem,ipsum,dolor,sit,amet" by default).
  * Environment variable WORDS.
  *
  * @author Pavel Tomskikh
  */
object PerformanceBenchmarkRunner extends App {
  println(Calendar.getInstance().getTime)

  private val config = ConfigFactory.load()
  private val mongoPort = config.getInt(mongoPortConfig)
  private val zkPort = config.getInt(zooKeeperPort)
  private val zkHost = config.getString(zooKeeperHost)
  private val kafkaAddress = config.getString(kafkaAddressConfig)
  private val messagesCount = config.getLong(messagesCountConfig)
  private val instanceName = config.getString(EngineConfigNames.instanceName)
  private val words = config.getString(wordsConfig).split(",")
  private val outputFileName = config.getString(outputFileConfig)
  private val messageSizes = config.getString(messageSizesConfig).split(",").map(_.toLong)

  private val performanceBenchmark = new PerformanceBenchmark(
    mongoPort,
    zkHost,
    zkPort,
    kafkaAddress,
    messagesCount,
    instanceName,
    words,
    outputFileName)(SjModule.injector)

  performanceBenchmark.startServices()

  performanceBenchmark.prepare()

  messageSizes.foreach(performanceBenchmark.runTest)

  performanceBenchmark.stopServices()

  println("DONE")
  println(Calendar.getInstance().getTime)

  System.exit(0)
}
