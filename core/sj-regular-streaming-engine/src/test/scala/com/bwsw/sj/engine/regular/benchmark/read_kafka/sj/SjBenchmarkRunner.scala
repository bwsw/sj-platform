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
package com.bwsw.sj.engine.regular.benchmark.read_kafka.sj

import java.util.Calendar

import com.bwsw.sj.common.SjModule
import com.bwsw.sj.common.engine.core.config.EngineConfigNames
import com.bwsw.sj.common.utils.BenchmarkConfigNames._
import com.bwsw.sj.common.utils.BenchmarkLiterals.sjDefaultOutputFile
import com.bwsw.sj.common.utils.CommonAppConfigNames.{zooKeeperHost, zooKeeperPort}
import com.bwsw.sj.engine.regular.benchmark.read_kafka.{KafkaReaderBenchmarkConfig, KafkaReaderBenchmarkRunner}
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}

/**
  * Performs [[SjBenchmark]].
  *
  * Configuration:
  *
  * sj-benchmark.performance.message.sizes - list of messages' sizes that separated by a comma (',').
  * Environment variable MESSAGE_SIZES.
  * sj-benchmark.performance.message.counts - list of counts of messages per test (1000000 by default).
  * Counts separated by a comma (','). Environment variable MESSAGES_COUNTS.
  * sj-benchmark.performance.kafka.address - Kafka server's address. Environment variable KAFKA_ADDRESS.
  * sj-benchmark.performance.mongo.port - port for embedded mongo server. Environment variable MONGO_PORT.
  * sj-benchmark.performance.output-file - file to output results in csv format (message size, milliseconds)
  * (sj-benchmark-output-`<`date-time`>` by default). Environment variable OUTPUT_FILE.
  * sj-benchmark.performance.words - list of words that sends to the Kafka server ("lorem,ipsum,dolor,sit,amet" by default).
  * Environment variable WORDS.
  * sj-benchmark.performance.repetitions - count of repetitions of same test configuration (messages count and message size)
  * (1 by default). Environment variable REPETITIONS.
  *
  * sj-common.zookeeper.host - ZooKeeper server's host. Environment variable ZOOKEEPER_HOST.
  * sj-common.zookeeper.port - ZooKeeper server's port. Environment variable ZOOKEEPER_PORT.
  * Host and port must point to the ZooKeeper server that used by the Kafka server.
  *
  * @author Pavel Tomskikh
  */
object SjBenchmarkRunner extends App {
  println(Calendar.getInstance().getTime)

  private val config: Config = ConfigFactory.load()
  private val mongoPort = config.getInt(mongoPortConfig)
  private val zkPort = config.getInt(zooKeeperPort)
  private val zkHost = config.getString(zooKeeperHost)
  private val instanceName = config.getString(EngineConfigNames.instanceName)

  private val benchmarkConfig = new KafkaReaderBenchmarkConfig(
    config = config.withValue(zooKeeperAddressConfig, ConfigValueFactory.fromAnyRef(s"$zkHost:$zkPort")),
    sjDefaultOutputFile)

  private val benchmark = new SjBenchmark(
    mongoPort,
    zkHost,
    zkPort,
    benchmarkConfig.kafkaAddress,
    instanceName,
    benchmarkConfig.words)(SjModule.injector)

  benchmark.startServices()
  benchmark.prepare()

  private val benchmarkRunner = new KafkaReaderBenchmarkRunner(benchmark, benchmarkConfig)
  private val results = benchmarkRunner.run()
  benchmarkRunner.writeResults(results)

  private val resultsString = results.mkString("\n")

  println("DONE")
  println("Results:")
  println(resultsString)

  println(Calendar.getInstance().getTime)

  System.exit(0)
}
