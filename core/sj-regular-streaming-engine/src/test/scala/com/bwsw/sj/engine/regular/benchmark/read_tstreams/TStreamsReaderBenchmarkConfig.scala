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

import com.bwsw.sj.common.utils.BenchmarkConfigNames._
import com.bwsw.sj.engine.core.testutils.benchmark.ReaderBenchmarkConfig
import com.typesafe.config.Config

/**
  * Loads the config parameters from typesafe config for [[TStreamsReaderBenchmark]]
  *
  * @param config               typesafe config
  * @param outputFilenamePrefix prefix for default name of output file
  * @author Pavel Tomskikh
  */
class TStreamsReaderBenchmarkConfig(override protected val config: Config,
                                    override protected val outputFilenamePrefix: String)
  extends ReaderBenchmarkConfig {

  val tStreamsPrefix = config.getString(tStreamsPrefixConfig)
  val tStreamsToken = config.getString(tStreamsTokenConfig)
  val sizePerTransaction = config.getString(tStreamsSizePerTransaction)
}
