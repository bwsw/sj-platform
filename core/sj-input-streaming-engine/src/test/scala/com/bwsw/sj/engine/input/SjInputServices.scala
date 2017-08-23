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

import java.io.File

import com.bwsw.sj.engine.input.DataFactory.connectionRepository

/**
  * @author Pavel Tomskikh
  */
object SjInputServices {
  val streamService = connectionRepository.getStreamRepository
  val serviceManager = connectionRepository.getServiceRepository
  val providerService = connectionRepository.getProviderRepository
  val instanceService = connectionRepository.getInstanceRepository
  val fileStorage = connectionRepository.getFileStorage

  val host = "localhost"
  val port = 8888
  val inputModule = new File("./contrib/stubs/sj-stub-input-streaming/target/scala-2.12/sj-stub-input-streaming-1.0-SNAPSHOT.jar")
  val checkpointInterval = 10
  val numberOfDuplicates = 10
  val totalInputElements = 2 * checkpointInterval + numberOfDuplicates // increase/decrease a constant to change the number of input elements
}
