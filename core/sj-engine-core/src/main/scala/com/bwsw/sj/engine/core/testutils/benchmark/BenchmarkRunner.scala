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
package com.bwsw.sj.engine.core.testutils.benchmark

import java.io.{File, FileWriter}
import java.util.Calendar

import com.bwsw.sj.engine.core.testutils.benchmark.loader.{BenchmarkDataSender, BenchmarkDataSenderParameters}

/**
  * Provides methods for running [[Benchmark]] and writing a result into a file
  *
  * @author Pavel Tomskikh
  */
class BenchmarkRunner[T <: BenchmarkParameters](config: BenchmarkRunnerConfig,
                                                sender: BenchmarkDataSender[BenchmarkDataSenderParameters],
                                                benchmark: Benchmark[T]) {
  benchmark.prepare()

  def run(): Iterable[Results] = {
    sender.warmUp()
    benchmark.warmUp(sender.warmingUpMessagesCount)
    sender.clearStorage()

    sender.flatMap { senderParameters =>
      printWithTime(s"Sender parameters: ${senderParameters.toSeq.mkString(",")}")

      benchmark.map { benchmarkParameters =>
        printWithTime(s"Benchmark parameters: ${benchmarkParameters.toSeq.mkString(",")}")
        val results = (1 to config.repetitions).map { _ =>
          val result = benchmark.run(benchmarkParameters, senderParameters.messageCount)
          printWithTime(result)

          result
        }

        Results(senderParameters, benchmarkParameters, results)
      }
    }
  }

  def writeResult(benchmarkResults: Iterable[Results]) = {
    val writer = new FileWriter(new File(config.outputFileName))
    writer.write(benchmarkResults.mkString("\n"))
    writer.close()
  }

  def stop(): Unit = benchmark.stop()

  private def printWithTime(a: Any): Unit =
    println(s"[${Calendar.getInstance().getTime}] $a")
}

case class Results(dataLoaderParams: BenchmarkDataSenderParameters,
                   moduleBenchParams: BenchmarkParameters,
                   results: Seq[Long]) {
  val averageResult: Long = results.sum / results.length

  def toSeq: Seq[Any] =
    dataLoaderParams.toSeq ++ moduleBenchParams.toSeq ++ results :+ averageResult

  override def toString: String = toSeq.mkString(",")
}
