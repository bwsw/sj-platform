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
package com.bwsw.sj.engine.batch.benchmark.read_kafka.flink

import com.bwsw.sj.engine.core.testutils.Constants
import com.bwsw.sj.engine.core.testutils.benchmark.batch.{BatchBenchmark, BatchBenchmarkConfig, BatchBenchmarkParameters}
import com.bwsw.sj.engine.core.testutils.benchmark.loader.kafka.KafkaBenchmarkDataSenderConfig

import scala.collection.JavaConverters._

/**
  * Provides methods for testing the speed of reading data by [[https://flink.apache.org Apache Flink]] from Kafka in
  * windowed mode.
  *
  * Topic deletion must be enabled on the Kafka server.
  *
  * @param benchmarkConfig configuration of application
  * @param senderConfig    configuration of Kafka topic
  * @author Pavel Tomskikh
  */
class FlinkBenchmark(benchmarkConfig: BatchBenchmarkConfig,
                     senderConfig: KafkaBenchmarkDataSenderConfig)
  extends BatchBenchmark(benchmarkConfig) {

  private val taskJarPath = "../../contrib/benchmarks/flink-batch-benchmark-task/target/scala-2.11/" +
    s"flink-batch-benchmark-task-${Constants.sjVersion}.jar"

  override protected def runProcess(parameters: BatchBenchmarkParameters, messagesCount: Long): Process = {
    val arguments = Seq(
      s"--messagesCount", messagesCount,
      s"--outputFile", outputFile.getAbsolutePath,
      s"--batchSize", parameters.batchSize,
      s"--windowSize", parameters.windowSize,
      s"--slidingInterval", parameters.slidingInterval,
      s"--topic", senderConfig.topic,
      "--bootstrap.servers", senderConfig.kafkaAddress,
      "--zookeeper.connect", senderConfig.zooKeeperAddress,
      "--from-beginning")

    val command = Seq("java", "-jar", taskJarPath) ++ arguments.map(_.toString)

    val process = new ProcessBuilder(command.asJava).start()
    println("Flink Application started")

    process
  }
}
