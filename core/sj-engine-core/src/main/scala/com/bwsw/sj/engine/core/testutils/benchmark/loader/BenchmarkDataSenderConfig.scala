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
package com.bwsw.sj.engine.core.testutils.benchmark.loader

import com.bwsw.sj.common.utils.BenchmarkConfigNames.{messageSizesConfig, messagesCountsConfig, wordsConfig}
import com.typesafe.config.Config

/**
  * Contains configuration of storage
  *
  * @author Pavel Tomskikh
  */
trait BenchmarkDataSenderConfig {
  protected val config: Config

  val messagesCounts = config.getString(messagesCountsConfig).split(",").map(_.toLong)
  val messageSizes = config.getString(messageSizesConfig).split(",").map(_.toLong)
  val words = config.getString(wordsConfig).split(",")
}
