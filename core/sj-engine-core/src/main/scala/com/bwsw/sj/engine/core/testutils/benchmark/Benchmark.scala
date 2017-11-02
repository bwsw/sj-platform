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

import java.io.{BufferedReader, File, FileReader}
import java.util.UUID

/**
  * Provides methods for testing the speed of handling data by some application.
  * Subclasses must iterate through all combination of application parameters.
  *
  * @param benchmarkConfig configuration of application
  * @tparam T type of application parameters
  * @author Pavel Tomskikh
  */
abstract class Benchmark[T <: BenchmarkParameters](benchmarkConfig: BenchmarkConfig) extends Iterable[T] {

  protected val warmingUpParams: T
  protected val lookupResultTimeout: Long = 5000
  protected val outputFilename: String = "benchmark-output-" + UUID.randomUUID().toString
  protected val outputFile: File = new File(outputFilename)

  /**
    * Performs the first test because it needs more time than subsequent tests
    */
  def warmUp(warmingUpMessagesCount: Long): Long =
    run(warmingUpParams, warmingUpMessagesCount)

  /**
    * Performs some preparations before all tests
    */
  def start(): Unit = {}

  /**
    * Tests an application with specific parameters
    *
    * @param benchmarkParameters specific parameters
    * @param messagesCount       count of messages
    * @return time in milliseconds within which an application under test handle messageCount messages
    */
  def run(benchmarkParameters: T, messagesCount: Long): Long = {
    val start = System.currentTimeMillis()

    def timeoutExceeded: Boolean =
      System.currentTimeMillis() - start > benchmarkConfig.timeoutPerTest

    val process = runProcess(benchmarkParameters, messagesCount)

    var maybeResult: Option[Long] = retrieveResultFromFile()
    while (maybeResult.isEmpty && process.isAlive && !timeoutExceeded) {
      Thread.sleep(lookupResultTimeout)
      maybeResult = retrieveResultFromFile()
    }

    if (process.isAlive)
      process.destroy()

    maybeResult.getOrElse(-1L)
  }

  /**
    * Runs an application under test with specific parameters in separate process
    *
    * @param benchmarkParameters specific parameters
    * @param messagesCount       count of messages
    * @return process with application under test
    */
  protected def runProcess(benchmarkParameters: T, messagesCount: Long): Process

  /**
    * Closes opened connections, deletes temporary files
    */
  def stop(): Unit = {}

  /**
    * Retrieves result from file
    *
    * @return result if a file exists or None otherwise
    */
  private def retrieveResultFromFile(): Option[Long] = {
    if (outputFile.exists() && outputFile.length() > 0) {
      val reader = new BufferedReader(new FileReader(outputFile))
      val result = reader.readLine()
      reader.close()

      if (Option(result).exists(_.nonEmpty)) {
        outputFile.delete()

        Some(result.toLong)
      } else
        None
    }
    else
      None
  }
}
