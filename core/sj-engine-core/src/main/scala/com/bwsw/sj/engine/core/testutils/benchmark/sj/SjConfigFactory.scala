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
package com.bwsw.sj.engine.core.testutils.benchmark.sj

import com.bwsw.sj.common.utils.BenchmarkConfigNames.zooKeeperAddressConfig
import com.bwsw.sj.common.utils.CommonAppConfigNames.{zooKeeperHost, zooKeeperPort}
import com.bwsw.sj.engine.core.testutils.benchmark.ConfigFactory
import com.typesafe.config.{Config, ConfigValueFactory}

/**
  * Provides method to create typesafe config instance for SJ benchmarks
  *
  * @author Pavel Tomskikh
  */
object SjConfigFactory extends ConfigFactory {

  /**
    * Creates typesafe config instance for SJ benchmarks
    *
    * @return typesafe config instance for SJ benchmarks
    */
  override def createConfig: Config = {
    val config = com.typesafe.config.ConfigFactory.load()
    val zkPort = config.getInt(zooKeeperPort)
    val zkHost = config.getString(zooKeeperHost)

    config.withValue(zooKeeperAddressConfig, ConfigValueFactory.fromAnyRef(s"$zkHost:$zkPort"))
  }
}
