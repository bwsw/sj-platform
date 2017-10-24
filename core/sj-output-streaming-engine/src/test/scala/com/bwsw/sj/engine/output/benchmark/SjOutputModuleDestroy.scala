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

import scala.util.Try

/**
  *
  *
  * @author Kseniya Tomskikh
  *
  */
object SjESOutputModuleDestroy extends App {
  val module = new File("./contrib/stubs/sj-stub-es-output-streaming/target/scala-2.12/sj-stub-es-output-streaming-1.0-SNAPSHOT.jar")

  deleteIndex()
  deleteStreams()
  deleteServices()
  deleteProviders()
  deleteInstance(esInstanceName)
  deleteModule(module.getName)
  close()
  tempHelperForConfigDestroy.deleteConfigs()
  connectionRepository.close()

  println("DONE")
}

object SjJDBCOutputModuleDestroy extends App {
  val jdbcModule = new File("./contrib/stubs/sj-stub-jdbc-output-streaming/target/scala-2.12/sj-stub-jdbc-output-streaming-1.0-SNAPSHOT.jar")

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

  println("DONE")
}

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
