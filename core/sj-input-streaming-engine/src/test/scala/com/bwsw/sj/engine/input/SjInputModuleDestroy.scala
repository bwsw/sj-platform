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
package com.bwsw.sj.engine.input

import java.util.logging.LogManager

import com.bwsw.sj.common.config.TempHelperForConfigDestroy
import com.bwsw.sj.common.utils.benchmark.ProcessTerminator
import com.bwsw.sj.engine.input.DataFactory._

object SjInputModuleDestroy extends App {
  ProcessTerminator.terminateProcessAfter { () =>
    LogManager.getLogManager.reset()

    deleteStreams(SjInputServices.streamService, outputCount)
    deleteServices(SjInputServices.serviceManager)
    deleteProviders(SjInputServices.providerService)
    deleteInstance(SjInputServices.instanceService)
    deleteModule(SjInputServices.fileStorage, SjInputModuleBenchmarkConstants.inputModule.getName)

    val tempHelperForConfigDestroy = new TempHelperForConfigDestroy(connectionRepository)
    tempHelperForConfigDestroy.deleteConfigs()
    connectionRepository.close()
  }
}