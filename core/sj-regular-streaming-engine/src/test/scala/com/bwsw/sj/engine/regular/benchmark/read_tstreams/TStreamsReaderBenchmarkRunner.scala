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
package com.bwsw.sj.engine.regular.benchmark.read_tstreams

import java.util.{Calendar, Date}

import com.bwsw.sj.common.utils.BenchmarkConfigNames._
import com.bwsw.sj.common.utils.BenchmarkLiterals.Regular.sjTStreamsDefaultOutputFile
import com.bwsw.sj.common.utils.CommonAppConfigNames.{zooKeeperHost, zooKeeperPort}
import com.bwsw.sj.engine.core.testutils.benchmark.ReaderBenchmarkRunner
import com.bwsw.sj.engine.core.testutils.benchmark.regular.RegularReaderBenchmarkRunner
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}

/**
  * Performs [[TStreamsReaderBenchmark]].
  *
  * Configuration:
  *
  * sj-benchmark.performance.message.sizes - list of messages' sizes that separated by a comma (',').
  * Environment variable MESSAGES_SIZE_PER_TEST.
  *
  * sj-benchmark.performance.message.counts - list of counts of messages per test (1000000 by default).
  * Counts separated by a comma (','). Environment variable MESSAGES_COUNT_PER_TEST.
  *
  * sj-benchmark.performance.output-file - file to output results in csv format (message size, milliseconds)
  * (sj-benchmark-output-`<`date-time`>` by default). Environment variable OUTPUT_FILE.
  *
  * sj-benchmark.performance.words - list of words that sends to the Kafka server ("lorem,ipsum,dolor,sit,amet" by default).
  * Environment variable WORDS.
  *
  * sj-benchmark.performance.repetitions - count of repetitions of same test configuration (messages count and message size)
  * (1 by default). Environment variable REPETITIONS.
  *
  * sj-benchmark.performance.tstreams.prefix - ZooKeeper root node which holds coordination tree. Environment variable PREFIX.
  *
  * sj-benchmark.performance.tstreams.token - T-Streams authentication token. Environment variable TOKEN.
  *
  * sj-common.zookeeper.host - ZooKeeper server's host. Environment variable ZOOKEEPER_HOST.
  *
  * sj-common.zookeeper.port - ZooKeeper server's port. Environment variable ZOOKEEPER_PORT.
  *
  * Host and port must point to the ZooKeeper server that used by the T-Streams server.
  *
  * @author Pavel Tomskikh
  */
object TStreamsReaderBenchmarkRunner extends App {
  println(Calendar.getInstance().getTime)

  private val config: Config = ConfigFactory.load()
  private val zkPort = config.getInt(zooKeeperPort)
  private val zkHost = config.getString(zooKeeperHost)

  private val benchmarkConfig = new TStreamsReaderBenchmarkConfig(
    config = config.withValue(zooKeeperAddressConfig, ConfigValueFactory.fromAnyRef(s"$zkHost:$zkPort")),
    sjTStreamsDefaultOutputFile)

  private val benchmark = new TStreamsReaderBenchmark(
    zkHost,
    zkPort,
    benchmarkConfig.tStreamsToken,
    benchmarkConfig.tStreamsPrefix,
    benchmarkConfig.words)

  benchmark.startServices()
  benchmark.prepare()

  private val benchmarkRunner = new RegularReaderBenchmarkRunner(benchmark, benchmarkConfig)
  private val results = benchmarkRunner.run()
  benchmarkRunner.writeResult(results)

  private val resultsString = results.mkString("\n")

  println("DONE")
  println("Results:")
  println(resultsString)

  println(Calendar.getInstance().getTime)

  System.exit(0)
}

class TStreamsReaderBenchmarkRunner(benchmark: TStreamsReaderBenchmark,
                                    config: TStreamsReaderBenchmarkConfig)
  extends ReaderBenchmarkRunner(config) {
  override def run(): Seq[TStreamsReaderBenchmarkResult] = {
    benchmark.warmUp()

    val benchmarkResults = config.messageSizes.flatMap { messageSize =>
      println(s"Message size: $messageSize")

      config.sizePerTransaction.flatMap { sizePerTransaction =>
        println(s"Transaction size: $sizePerTransaction")
        benchmark.clearStorage()
        var streamSize: Long = 0

        config.messagesCounts.map { messagesCount =>
          println(s"Messages count: $messagesCount")
          if (messagesCount > streamSize) {
            benchmark.sendData(messageSize, messagesCount - streamSize, sizePerTransaction)
            streamSize = messagesCount
          }

          val result = (0 until config.repetitions).map { _ =>
            val millis = benchmark.runTest(messagesCount)
            println(s"[${new Date()}] $millis")

            millis
          }

          TStreamsReaderBenchmarkResult(messageSize, messagesCount, sizePerTransaction, result)
        }
      }
    }

    benchmark.stop()

    benchmarkResults
  }
}
