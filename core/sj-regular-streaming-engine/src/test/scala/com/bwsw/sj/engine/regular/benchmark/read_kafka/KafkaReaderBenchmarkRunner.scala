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
package com.bwsw.sj.engine.regular.benchmark.read_kafka

import java.io.{File, FileWriter}

/**
  * Provides methods for running [[KafkaReaderBenchmark]] and writing a result into a file
  *
  * @param benchmark benchmark
  * @param config    config for benchmark
  * @author Pavel Tomskikh
  */
class KafkaReaderBenchmarkRunner(benchmark: KafkaReaderBenchmark,
                                 config: KafkaReaderBenchmarkConfig) {

  def run(): Array[KafkaReaderBenchmarkResult] = {
    benchmark.warmUp()

    val benchmarkResults = config.messagesCounts.flatMap { messagesCount =>
      config.messageSizes.map { messageSize =>
        val result = (0 until config.repetitions).map(_ => benchmark.runTest(messageSize, messagesCount))

        KafkaReaderBenchmarkResult(messageSize, messagesCount, result)
      }
    }

    benchmark.close()

    benchmarkResults
  }

  def writeResult(benchmarkResults: Seq[KafkaReaderBenchmarkResult]) = {
    val writer = new FileWriter(new File(config.outputFileName))
    writer.write(benchmarkResults.mkString("\n"))
    writer.close()
  }
}

case class KafkaReaderBenchmarkResult(messageSize: Long, messagesCount: Long, results: Seq[Long]) {
  def averageResult: Long =
    results.sum / results.length

  override def toString: String =
    s"$messagesCount,$messageSize,${results.mkString(",")},$averageResult"
}
