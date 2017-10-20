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
package com.bwsw.sj.engine.batch.module

import java.io.File
import java.util.logging.LogManager

import com.bwsw.sj.common.config.TempHelperForConfigSetup
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.benchmark.ProcessTerminator
import com.bwsw.sj.engine.batch.module.DataFactory._
import com.bwsw.sj.engine.batch.module.SjBatchModuleBenchmarkConstants._

object SjBatchModuleSetup extends App {
  ProcessTerminator.terminateProcessAfter { () =>
    LogManager.getLogManager.reset()
    val tempHelperForConfigSetup = new TempHelperForConfigSetup(connectionRepository)
    tempHelperForConfigSetup.setupConfigs()
    val streamService = connectionRepository.getStreamRepository
    val serviceManager = connectionRepository.getServiceRepository
    val providerService = connectionRepository.getProviderRepository
    val instanceService = connectionRepository.getInstanceRepository
    val fileStorage = connectionRepository.getFileStorage
    val stateManagement = EngineLiterals.ramStateMode
    val totalInputElements = defaultValueOfTxns * defaultValueOfElements * inputCount * {
      if (inputStreamsType == commonMode) 2
      else 1
    }

    val module = new File(modulePath)

    loadModule(module, fileStorage)
    createProviders(providerService)
    createServices(serviceManager, providerService)
    createStreams(streamService, serviceManager, partitions, inputStreamsType, inputCount, outputCount)
    createInstance(serviceManager, instanceService, window, slidingInterval, totalInputElements, stateManagement, stateFullCheckpoint)

    createData(defaultValueOfTxns, defaultValueOfElements, streamService, inputStreamsType, inputCount)
    connectionRepository.close()
  }
}

class SjBatchModuleSetup
