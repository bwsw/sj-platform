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

import com.bwsw.sj.common.config.TempHelperForConfigSetup
import com.bwsw.sj.engine.input.DataFactory._
import com.bwsw.sj.engine.input.SjInputModuleBenchmarkConstants.{checkpointInterval, inputModule}

import scala.util.{Failure, Success, Try}

/**
  * @author Pavel Tomskikh
  */
object SjInputModuleSetup extends App {
  LogManager.getLogManager.reset()

  val exitCode = Try {
    val tempHelperForConfigSetup = new TempHelperForConfigSetup(connectionRepository)
    tempHelperForConfigSetup.setupConfigs()
    println("config loaded")

    loadModule(inputModule, SjInputServices.fileStorage)
    println("module loaded")
    createProviders(SjInputServices.providerService)
    println("providers created")
    createServices(SjInputServices.serviceManager, SjInputServices.providerService)
    println("services created")
    createStreams(SjInputServices.streamService, SjInputServices.serviceManager, outputCount)
    println("streams created")
    createInstance(SjInputServices.serviceManager, SjInputServices.instanceService, checkpointInterval)
    println("instances created")

    connectionRepository.close()
  } match {
    case Success(_) =>
      println("DONE")
      0

    case Failure(e) =>
      e.printStackTrace()
      1
  }

  System.exit(exitCode)
}

class SjInputModuleSetup
