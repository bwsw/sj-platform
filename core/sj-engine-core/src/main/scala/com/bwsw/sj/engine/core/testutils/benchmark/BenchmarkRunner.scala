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

import com.bwsw.sj.engine.core.testutils.benchmark.loader.{BenchmarkDataSenderConfig, BenchmarkDataSenderParameters, SenderFactory}

/**
  * Provides methods for running [[Benchmark]] and writing a result into a file
  *
  * @author Pavel Tomskikh
  */
class BenchmarkRunner[T <: BenchmarkParameters, S <: BenchmarkDataSenderParameters, C <: BenchmarkDataSenderConfig]
(configFactory: ConfigFactory,
 outputFilenamePrefix: String,
 senderFactory: SenderFactory[S, C],
 benchmarkFactory: BenchmarkFactory[T, C])
  extends App {

  println(Calendar.getInstance().getTime)

  private val config = configFactory.createConfig
  private val runnerConfig = new BenchmarkRunnerConfig(config, outputFilenamePrefix)
  private val (sender, senderConfig) = senderFactory.create(config)
  private val benchmark = benchmarkFactory.create(config, senderConfig)

  benchmark.prepare()

  private val results = run()
  writeResult(results)
  benchmark.stop()

  private val resultsString = results.mkString("\n")

  println("DONE")
  println("Results:")
  println(resultsString)

  println(Calendar.getInstance().getTime)

  System.exit(0)


  def run(): Iterable[Results] = {
    sender.warmUp()
    benchmark.warmUp(sender.warmingUpParameters.messagesCount)

    sender.flatMap { senderParameters =>
      printWithTime(s"Sender parameters: ${senderParameters.toSeq.mkString(",")}")
      sender.send(senderParameters)

      benchmark.map { benchmarkParameters =>
        printWithTime(s"Benchmark parameters: ${benchmarkParameters.toSeq.mkString(",")}")
        val results = (1 to runnerConfig.repetitions).map { _ =>
          val result = benchmark.run(benchmarkParameters, senderParameters.messagesCount)
          printWithTime(result)

          result
        }

        Results(senderParameters, benchmarkParameters, results)
      }
    }
  }

  def writeResult(benchmarkResults: Iterable[Results]) = {
    val writer = new FileWriter(new File(runnerConfig.outputFileName))
    writer.write(benchmarkResults.mkString("\n"))
    writer.close()
  }

  private def printWithTime(a: Any): Unit =
    println(s"[${Calendar.getInstance().getTime}] $a")
}

case class Results(dataLoaderParams: BenchmarkDataSenderParameters,
                   moduleBenchParams: BenchmarkParameters,
                   results: Seq[Long]) {
  val averageResult: Long = {
    val successResults = results.filter(_ >= 0)

    if (successResults.nonEmpty) successResults.sum / successResults.length
    else -1L
  }

  def toSeq: Seq[Any] =
    dataLoaderParams.toSeq ++ moduleBenchParams.toSeq ++ results :+ averageResult

  override def toString: String = toSeq.mkString(",")
}
