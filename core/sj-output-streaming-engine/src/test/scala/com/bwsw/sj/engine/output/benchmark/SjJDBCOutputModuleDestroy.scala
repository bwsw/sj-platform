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

import com.bwsw.sj.common.utils.benchmark.ProcessTerminator
import com.bwsw.sj.engine.output.benchmark.DataFactory._
import com.bwsw.sj.engine.output.benchmark.SjOutputModuleBenchmarkConstants.{jdbcInstanceName, pathToJdbcModule}

import scala.util.Try

/**
  * @author Kseniya Tomskikh
  */
object SjJDBCOutputModuleDestroy extends App {
  ProcessTerminator.terminateProcessAfter { () =>
    val jdbcModule = new File(pathToJdbcModule)

    Try(clearDatabase())
    deleteStreams()
    deleteServices()
    deleteProviders()
    deleteInstance(jdbcInstanceName)
    deleteModule(jdbcModule.getName)

    close()
    tempHelperForConfigDestroy.deleteJdbcDriver()
    tempHelperForConfigDestroy.deleteConfigs()
    connectionRepository.close()
  }
}
