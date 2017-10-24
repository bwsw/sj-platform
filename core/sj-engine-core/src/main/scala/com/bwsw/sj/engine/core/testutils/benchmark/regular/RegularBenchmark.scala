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

import com.bwsw.sj.engine.core.testutils.benchmark.{Benchmark, BenchmarkConfig, BenchmarkParameters}

/**
  * Provides methods for testing the speed of handling data by some application
  *
  * @param benchmarkConfig configuration of application
  * @author Pavel Tomskikh
  */
abstract class RegularBenchmark(benchmarkConfig: BenchmarkConfig)
  extends Benchmark[BenchmarkParameters](benchmarkConfig) {
  override protected val warmingUpParams: BenchmarkParameters = RegularBenchmarkParameters

  override def iterator: Iterator[BenchmarkParameters] =
    Seq(RegularBenchmarkParameters).iterator


  /**
    * Runs an application under test with specific parameters in separate process
    *
    * @param benchmarkParameters specific parameters
    * @param messagesCount       count of messages
    * @return process with application under test
    */
  override protected def runProcess(benchmarkParameters: BenchmarkParameters, messagesCount: Long): Process =
    runProcess(messagesCount)


  /**
    * Runs an application under test in separate process
    *
    * @param messagesCount count of messages
    * @return process with application under test
    */
  protected def runProcess(messagesCount: Long): Process
}
