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
package com.bwsw.sj.engine.batch.benchmark.read_kafka.storm

import java.util.Calendar

import com.bwsw.sj.common.utils.BenchmarkLiterals.stormDefaultOutputFile
import com.bwsw.sj.engine.core.testutils.benchmark.read_kafka.batch.{BatchKafkaReaderBenchmarkConfig, BatchKafkaReaderBenchmarkRunner}
import com.typesafe.config.ConfigFactory

/**
  * Performs [[StormBenchmark]]
  *
  * Configuration:
  *
  * sj-benchmark.performance.message.sizes - list of messages' sizes that separated by a comma (',').
  * Environment variable MESSAGES_SIZE_PER_TEST.
  *
  * sj-benchmark.performance.message.counts - list of counts of messages per test (1000000 by default).
  * Counts separated by a comma (','). Environment variable MESSAGES_COUNT_PER_TEST.
  *
  * sj-benchmark.performance.kafka.address - Kafka server's address. Environment variable KAFKA_ADDRESS.
  *
  * sj-benchmark.performance.zookeeper.address - ZooKeeper server's address. Must point to the ZooKeeper server that used
  * by the Kafka server. Environment variable ZOOKEEPER_ADDRESS.
  *
  * sj-benchmark.performance.output-file - file to output results in csv format (message size, milliseconds)
  * (storm-benchmark-output-`<`date-time`>` by default). Environment variable OUTPUT_FILE.
  *
  * sj-benchmark.performance.words - list of words that sends to the Kafka server ("lorem,ipsum,dolor,sit,amet" by default).
  * Environment variable WORDS.
  *
  * sj-benchmark.performance.repetitions - count of repetitions of same test configuration (messages count and message size)
  * (1 by default). Environment variable REPETITIONS.
  *
  * sj-benchmark.performance.batch.sizes - list of batches' sizes in milliseconds that separated by a comma (',').
  * Environment variable BATCH_SIZE_PER_TEST.
  *
  * sj-benchmark.performance.batch.window.sizes - list of windows' sizes that separated by a comma (',').
  * Environment variable WINDOW_SIZE_PER_TEST.
  *
  * sj-benchmark.performance.batch.sliding.intervals - list of sliding intervals that separated by a comma (',').
  * 0 means that sliding interval equal to a window size. Environment variable SLIDING_INTERVAL.
  *
  * @author Pavel Tomskikh
  */
object StormBenchmarkRunner extends App {
  println(Calendar.getInstance().getTime)

  private val benchmarkConfig = new BatchKafkaReaderBenchmarkConfig(ConfigFactory.load(), stormDefaultOutputFile)
  private val benchmark = new StormBenchmark(benchmarkConfig.zooKeeperAddress, benchmarkConfig.kafkaAddress, benchmarkConfig.words)
  private val benchmarkRunner = new BatchKafkaReaderBenchmarkRunner(benchmark, benchmarkConfig)

  private val results = benchmarkRunner.run()
  benchmarkRunner.writeResult(results)

  private val resultsString = results.mkString("\n")

  println("DONE")
  println("Results:")
  println(resultsString)

  println(Calendar.getInstance().getTime)
}
