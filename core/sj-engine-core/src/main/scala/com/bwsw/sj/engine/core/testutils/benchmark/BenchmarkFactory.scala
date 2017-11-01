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

import com.bwsw.sj.engine.core.testutils.benchmark.loader.BenchmarkDataSenderConfig
import com.typesafe.config.Config

/**
  * Provides method to create banchmark instance
  *
  * @author Pavel Tomskikh
  */
trait BenchmarkFactory[T <: BenchmarkParameters, C <: BenchmarkDataSenderConfig] {

  /**
    * Creates benchmark instance
    *
    * @param config       typesafe config
    * @param senderConfig sender config
    * @return benchmark instance
    */
  def create(config: Config, senderConfig: C): Benchmark[T]
}
