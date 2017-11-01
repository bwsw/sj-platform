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
package com.bwsw.sj.engine.regular.benchmark.read_tstreams

import java.io.File
import java.util.Date

import com.bwsw.sj.common.dal.model.stream.TStreamStreamDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.engine.core.testutils.benchmark.sj.SjBenchmarkPreparation

/**
  * It is needed to upload SJ entities such as providers, services, streams, a module and an instance
  *
  * @author Pavel Tomskikh
  */
class SjTStreamsReaderBenchmarkPreparation(mongoPort: Int,
                                           zooKeeperHost: String,
                                           zooKeeperPort: Int,
                                           module: File,
                                           streamName: String,
                                           zkNamespace: String,
                                           tStreamsPrefix: String,
                                           tStreamsToken: String,
                                           instanceName: String,
                                           taskName: String)
  extends SjBenchmarkPreparation(
    mongoPort,
    zooKeeperHost,
    zooKeeperPort,
    module,
    zkNamespace,
    tStreamsPrefix,
    tStreamsToken,
    instanceName,
    taskName) {

  override val inputStream = new TStreamStreamDomain(
    name = streamName,
    service = tStreamsService,
    partitions = 1,
    creationDate = new Date())

  override val inputStreamPartitions = inputStream.partitions

  override protected def loadSpecificMetadata(connectionRepository: ConnectionRepository): Unit =
    connectionRepository.getServiceRepository.save(tStreamsService)
}
