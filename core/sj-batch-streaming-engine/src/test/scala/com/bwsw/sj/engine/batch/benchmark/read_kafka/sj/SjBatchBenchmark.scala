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
package com.bwsw.sj.engine.batch.benchmark.read_kafka.sj

import com.bwsw.sj.engine.core.testutils.TestStorageServer
import com.bwsw.sj.engine.core.testutils.benchmark.batch.{BatchBenchmark, BatchBenchmarkConfig, BatchBenchmarkParameters}
import com.bwsw.sj.engine.core.testutils.benchmark.loader.kafka.KafkaBenchmarkDataSenderConfig
import com.bwsw.sj.engine.core.testutils.benchmark.sj.KafkaInputStreamFactory

/**
  * Provides methods for testing the speed of reading data from Kafka by Stream Juggler.
  *
  * Topic deletion must be enabled on the Kafka server.
  *
  * Host and port must point to the ZooKeeper server that used by the Kafka server.
  *
  * @param benchmarkConfig configuration of application
  * @param senderConfig    configuration of Kafka topic
  * @author Pavel Tomskikh
  */
class SjBatchBenchmark(benchmarkConfig: BatchBenchmarkConfig,
                       senderConfig: KafkaBenchmarkDataSenderConfig)
  extends BatchBenchmark(benchmarkConfig) {

  private val kafkaInputStreamFactory = new KafkaInputStreamFactory(senderConfig.topic, senderConfig.kafkaAddress)
  private val helper = new SjBatchBenchmarkHelper(
    senderConfig.zooKeeperAddress,
    "benchmark",
    TestStorageServer.defaultPrefix,
    TestStorageServer.defaultToken,
    kafkaInputStreamFactory,
    true)

  override def start(): Unit =
    helper.start()

  override def stop(): Unit =
    helper.stop()

  override protected def runProcess(parameters: BatchBenchmarkParameters, messagesCount: Long): Process =
    helper.runProcess(parameters, messagesCount, outputFile.getAbsolutePath)
}
