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
package com.bwsw.sj.engine.core.testutils.benchmark.batch

import com.bwsw.sj.engine.core.testutils.benchmark.Benchmark

/**
  * Provides methods for testing the speed of reading data from a storage by some application in a batch mode
  *
  * @param benchmarkConfig configuration of application
  * @author Pavel Tomskikh
  */
abstract class BatchBenchmark(benchmarkConfig: BatchBenchmarkConfig)
  extends Benchmark[BatchBenchmarkParameters](benchmarkConfig) {

  override protected val warmingUpParams: BatchBenchmarkParameters = BatchBenchmarkParameters(1000, 1, 1)

  override def iterator: Iterator[BatchBenchmarkParameters] = {
    benchmarkConfig.batchSizes.flatMap(batchSize =>
      benchmarkConfig.windowSizes.flatMap(windowSize =>
        benchmarkConfig.slidingIntervals.map { slidingInterval =>
          if (slidingInterval != 0) slidingInterval
          else windowSize
        }.map(slidingInterval =>
          BatchBenchmarkParameters(batchSize, windowSize, slidingInterval)))).iterator
  }
}
