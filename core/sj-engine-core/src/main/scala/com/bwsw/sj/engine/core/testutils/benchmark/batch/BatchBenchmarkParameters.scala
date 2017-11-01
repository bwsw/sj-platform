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

import com.bwsw.sj.engine.core.testutils.benchmark.BenchmarkParameters

/**
  * Parameters of application in regular mode for specific test
  *
  * @param batchSize       batch size
  * @param windowSize      batches in one window
  * @param slidingInterval sliding interval
  * @author Pavel Tomskikh
  */
case class BatchBenchmarkParameters(batchSize: Int, windowSize: Int, slidingInterval: Int) extends BenchmarkParameters {

  /**
    * Returns list of parameters
    *
    * @return list of parameters
    */
  override def toSeq = Seq(batchSize, windowSize, slidingInterval)
}
