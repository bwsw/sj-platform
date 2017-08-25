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
package com.bwsw.sj.engine.output.benchmark

import java.io.File

import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.benchmark.BenchmarkUtils
import com.bwsw.sj.engine.output.benchmark.DataFactory._
import com.bwsw.sj.engine.output.benchmark.SjOutputModuleBenchmarkConstants._

/**
  * Environment for Prepare
  * MONGO_HOSTS=176.120.25.19:27017
  * AGENTS_HOST=176.120.25.19
  * AGENTS_PORTS=31000,31001
  * ZOOKEEPER_HOSTS=176.120.25.19:2181
  * ES_HOSTS=176.120.25.19:9300
  * JDBC_HOSTS=176.120.25.19:5432  -postgresql
  * RESTFUL_HOSTS=
  *
  * @author Pavel Tomskikh
  */
object SjRestOutputModuleSetup extends App {
  BenchmarkUtils.exitAfter { () =>
    tempHelperForConfigSetup.setupConfigs()
    val checkpointMode = EngineLiterals.everyNthMode
    val partitions = 4

    val restModule = new File(pathToRestModule)

    println("module upload")
    uploadModule(restModule)

    println("create providers")
    createProviders()
    println("create services")
    createServices()
    println("create streams")
    createStreams(partitions)
    println("create instance")

    createInstance(
      restInstanceName,
      checkpointMode,
      checkpointInterval,
      restStreamName,
      "com.bwsw.stub.output-bench-test-rest",
      totalInputElements)

    println("create test data")
    createData(countTxns, countElements)

    println("close connections")
    close()
    connectionRepository.close()

    println("DONE")
  }
}

class SjRestOutputModuleSetup