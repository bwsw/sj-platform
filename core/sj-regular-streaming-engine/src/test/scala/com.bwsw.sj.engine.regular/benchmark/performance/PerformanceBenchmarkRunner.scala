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
package com.bwsw.sj.engine.regular.benchmark.performance

import com.bwsw.sj.common.SjModule
import com.bwsw.sj.common.engine.core.config.EngineConfigNames
import com.bwsw.sj.common.utils.BenchmarkConfigNames._
import com.bwsw.sj.common.utils.CommonAppConfigNames.{mongoHosts, zooKeeperPort}
import com.typesafe.config.ConfigFactory

/**
  * @author Pavel Tomskikh
  */
object PerformanceBenchmarkRunner extends App {
  private val config = ConfigFactory.load()
  private val mongoPort = config.getString(mongoHosts).split(":")(1).toInt
  private val zkPort = config.getInt(zooKeeperPort)
  private val kafkaAddress = config.getString(kafkaAddressConfig)
  private val kafkaTopic = config.getString(kafkaTopicConfig)
  private val messagesCount = config.getLong(messagesCountConfig)
  private val instanceName = config.getString(EngineConfigNames.instanceName)
  private val words = config.getString(wordsConfig).split(",")
  private val outputFileName = config.getString(outputFileConfig)
  private val messageSizes = config.getString(messageSizesConfig).split(",").map(_.toLong)

  private val performanceBenchmark = new PerformanceBenchmark(
    mongoPort,
    zkPort,
    kafkaAddress,
    kafkaTopic,
    messagesCount,
    instanceName,
    words,
    outputFileName)(SjModule.injector)

  performanceBenchmark.prepare()

  performanceBenchmark.runTest(messageSizes.head)

  println("DONE")

  performanceBenchmark.stop()
}
