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

import java.util.Date

import com.bwsw.sj.engine.core.testutils.benchmark.read_kafka.{KafkaReaderBenchmarkConfig, KafkaReaderBenchmarkResult, KafkaReaderBenchmarkRunner}

/**
  * Provides methods for running [[RegularKafkaReaderBenchmark]] and writing a result into a file
  *
  * @param benchmark benchmark
  * @param config    config for benchmark
  * @author Pavel Tomskikh
  */
class RegularKafkaReaderBenchmarkRunner(benchmark: RegularKafkaReaderBenchmark,
                                        config: KafkaReaderBenchmarkConfig)
  extends KafkaReaderBenchmarkRunner(config) {

  def run(): Seq[RegularKafkaReaderBenchmarkResult] = {
    benchmark.warmUp()

    val benchmarkResults = config.messagesCounts.flatMap { messagesCount =>
      config.messageSizes.map { messageSize =>
        benchmark.sendData(messageSize, messagesCount)
        val result = (0 until config.repetitions).map { _ =>
          val millis = benchmark.runTest(messagesCount)
          println(s"[${new Date()}] $millis")

          millis
        }

        RegularKafkaReaderBenchmarkResult(messageSize, messagesCount, result)
      }
    }

    benchmark.close()

    benchmarkResults
  }
}

case class RegularKafkaReaderBenchmarkResult(messageSize: Long, messagesCount: Long, results: Seq[Long])
  extends KafkaReaderBenchmarkResult(results) {

  override def toString: String =
    s"$messagesCount,$messageSize,${results.mkString(",")},$averageResult"
}
