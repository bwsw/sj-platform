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

import com.bwsw.sj.common.utils.BenchmarkLiterals.Regular.sjDefaultOutputFile
import com.bwsw.sj.engine.core.testutils.benchmark.loader.kafka.{KafkaBenchmarkDataSender, KafkaBenchmarkDataSenderConfig}
import com.bwsw.sj.engine.core.testutils.benchmark.regular.RegularBenchmarkFactory
import com.bwsw.sj.engine.core.testutils.benchmark.sj.KafkaInputStreamFactory
import com.bwsw.sj.engine.core.testutils.benchmark.{BenchmarkConfig, BenchmarkRunner}
import com.bwsw.sj.engine.regular.benchmark.SjRegularBenchmark

/**
  * Performs [[SjRegularBenchmark]].
  *
  * Configuration:
  *
  * sj-benchmark.performance.message.sizes - list of messages' sizes separated by a comma (',').
  * Environment variable MESSAGES_SIZE_PER_TEST.
  *
  * sj-benchmark.performance.message.counts - list of counts of messages per test (1000000 by default).
  * Counts separated by a comma (','). Environment variable MESSAGES_COUNT_PER_TEST.
  *
  * sj-benchmark.performance.kafka.address - Kafka server's address. Environment variable KAFKA_ADDRESS.
  *
  * sj-benchmark.performance.zookeeper.address - ZooKeeper server's address. Must point to the ZooKeeper server used
  * by the Kafka server. Environment variable ZOOKEEPER_ADDRESS.
  *
  * sj-benchmark.performance.output-file - file to output results in csv format (message size, milliseconds)
  * (sj-regular-benchmark-output-`<`date-time`>` by default). Environment variable OUTPUT_FILE.
  *
  * sj-benchmark.performance.words - list of words that sends to the Kafka server ("lorem,ipsum,dolor,sit,amet" by default).
  * Environment variable WORDS.
  *
  * sj-benchmark.performance.repetitions - count of repetitions of same test configuration (messages count and message size)
  * (1 by default). Environment variable REPETITIONS.
  *
  * @author Pavel Tomskikh
  */
object SjBenchmarkRunner extends BenchmarkRunner(
  sjDefaultOutputFile,
  KafkaBenchmarkDataSender,
  SjBenchmarkFactory)

object SjBenchmarkFactory extends RegularBenchmarkFactory[KafkaBenchmarkDataSenderConfig] {
  override protected def create(benchmarkConfig: BenchmarkConfig,
                                senderConfig: KafkaBenchmarkDataSenderConfig): SjRegularBenchmark = {
    val kafkaInputStreamFactory = new KafkaInputStreamFactory(senderConfig.topic, senderConfig.kafkaAddress)

    new SjRegularBenchmark(benchmarkConfig, senderConfig, kafkaInputStreamFactory)
  }
}
