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
package com.bwsw.sj.engine.core.testutils.benchmark.regular

import java.util.Date

import com.bwsw.sj.engine.core.testutils.benchmark.{ReaderBenchmarkConfig, ReaderBenchmarkRunner}

/**
  * Provides methods for running [[RegularReaderBenchmark]] and writing a result into a file
  *
  * @param benchmark benchmark
  * @param config    config for benchmark
  * @author Pavel Tomskikh
  */
class RegularReaderBenchmarkRunner(benchmark: RegularReaderBenchmark,
                                   config: ReaderBenchmarkConfig)
  extends ReaderBenchmarkRunner(config) {

  def run(): Seq[RegularReaderBenchmarkResult] = {
    benchmark.warmUp()

    val benchmarkResults = config.messageSizes.flatMap { messageSize =>
      println(s"Message size: $messageSize")
      benchmark.clearStorage()
      var topicSize: Long = 0

      config.messagesCounts.map { messagesCount =>
        println(s"Messages count: $messagesCount")
        if (messagesCount > topicSize) {
          benchmark.sendData(messageSize, messagesCount - topicSize)
          topicSize = messagesCount
        }

        val result = (0 until config.repetitions).map { _ =>
          val millis = benchmark.runTest(messagesCount)
          println(s"[${new Date()}] $millis")

          millis
        }

        RegularReaderBenchmarkResult(messageSize, messagesCount, result)
      }
    }

    benchmark.stop()

    benchmarkResults
  }
}
