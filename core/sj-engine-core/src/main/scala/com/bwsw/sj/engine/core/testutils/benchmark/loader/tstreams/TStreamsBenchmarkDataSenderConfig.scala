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
package com.bwsw.sj.engine.core.testutils.benchmark.loader.tstreams

import java.util.UUID

import com.bwsw.sj.common.utils.BenchmarkConfigNames.{tStreamsPrefixConfig, tStreamsSizePerTransaction, tStreamsTokenConfig, zooKeeperAddressConfig}
import com.bwsw.sj.engine.core.testutils.benchmark.loader.BenchmarkDataSenderConfig
import com.typesafe.config.Config

/**
  * Contains configuration of T-Streams stream
  *
  * @author Pavel Tomskikh
  */
class TStreamsBenchmarkDataSenderConfig(override protected val config: Config) extends BenchmarkDataSenderConfig {
  val prefix: String = config.getString(tStreamsPrefixConfig)
  val token: String = config.getString(tStreamsTokenConfig)
  val sizePerTransaction: Array[Long] = config.getString(tStreamsSizePerTransaction).split(",").map(_.toLong)
  val stream: String = "performance-benchmark-" + UUID.randomUUID().toString
}
