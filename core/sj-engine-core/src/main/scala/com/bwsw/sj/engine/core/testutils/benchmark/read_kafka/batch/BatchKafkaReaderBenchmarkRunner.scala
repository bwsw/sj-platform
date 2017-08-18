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
package com.bwsw.sj.engine.core.testutils.benchmark.read_kafka.batch

import java.util.Date

import com.bwsw.sj.engine.core.testutils.benchmark.read_kafka.{KafkaReaderBenchmarkResult, KafkaReaderBenchmarkRunner}

/**
  * Provides methods for running [[BatchKafkaReaderBenchmark]] and writing a result into a file
  *
  * @param benchmark benchmark
  * @param config    config for benchmark
  * @author Pavel Tomskikh
  */
class BatchKafkaReaderBenchmarkRunner(benchmark: BatchKafkaReaderBenchmark,
                                      config: BatchKafkaReaderBenchmarkConfig)
  extends KafkaReaderBenchmarkRunner(config) {

  def run(): Seq[BatchKafkaReaderBenchmarkResult] = {
    benchmark.warmUp()

    val benchmarkResults = config.messageSizes.flatMap { messageSize =>
      println(s"Message size: $messageSize")
      benchmark.clearTopic()
      var topicSize: Long = 0

      config.messagesCounts.flatMap { messagesCount =>
        println(s"Messages count: $messagesCount")
        if (messagesCount > topicSize) {
          benchmark.sendData(messageSize, messagesCount - topicSize)
          topicSize = messagesCount
        }

        config.batchSizes.flatMap { batchSize =>
          config.windowSizes.flatMap { windowSize =>
            config.slidingIntervals.map { i =>
              if (i == 0) windowSize
              else i
            }.map { slidingInterval =>
              val result = (0 until config.repetitions).map { _ =>
                val millis = benchmark.runTest(messagesCount, batchSize, windowSize, slidingInterval)
                println(s"[${new Date()}] $millis")

                millis
              }

              BatchKafkaReaderBenchmarkResult(
                messageSize,
                messagesCount,
                batchSize,
                windowSize,
                slidingInterval,
                result)
            }
          }
        }
      }
    }

    benchmark.close()

    benchmarkResults
  }
}


case class BatchKafkaReaderBenchmarkResult(messageSize: Long,
                                           messagesCount: Long,
                                           batchSize: Int,
                                           windowSize: Int,
                                           slidingInterval: Int,
                                           results: Seq[Long])
  extends KafkaReaderBenchmarkResult(results) {

  override def toString: String =
    s"$messagesCount,$messageSize,$batchSize,$windowSize,$slidingInterval,${results.mkString(",")},$averageResult"
}
