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
import com.bwsw.sj.common.utils.benchmark.ProcessTerminator
import com.bwsw.sj.engine.output.benchmark.DataFactory._
import com.bwsw.sj.engine.output.benchmark.SjOutputModuleBenchmarkConstants._

/**
  * @author Pavel Tomskikh
  */
object SjRestOutputModuleSetup extends App {
  ProcessTerminator.terminateProcessAfter { () =>
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
  }
}

class SjRestOutputModuleSetup