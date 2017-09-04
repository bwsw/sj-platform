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

import com.bwsw.sj.engine.output.benchmark.DataFactory._
import com.bwsw.sj.engine.output.benchmark.SjOutputModuleBenchmarkConstants.{pathToRestModule, restInstanceName}

/**
  * MONGO_HOST=176.120.25.19:27017
  * AGENTS_HOST=176.120.25.19
  * AGENTS_PORTS=31000,31001
  * ZOOKEEPER_HOSTS=176.120.25.19:2181
  * ES_HOSTS=176.120.25.19:9300
  * JDBC_HOSTS=0.0.0.0:5432
  *
  * @author Pavel Tomskikh
  */
object SjRestOutputModuleDestroy extends App {
  val restModule = new File(pathToRestModule)

  deleteStreams()
  deleteServices()
  deleteProviders()
  deleteInstance(restInstanceName)
  deleteModule(restModule.getName)

  close()
  tempHelperForConfigDestroy.deleteConfigs()
  connectionRepository.close()

  println("DONE")
}