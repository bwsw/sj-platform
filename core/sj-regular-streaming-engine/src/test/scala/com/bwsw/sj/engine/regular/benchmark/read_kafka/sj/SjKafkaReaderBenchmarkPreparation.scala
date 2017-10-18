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
package com.bwsw.sj.engine.regular.benchmark.read_kafka.sj

import java.io.File
import java.util.Date

import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.model.service.KafkaServiceDomain
import com.bwsw.sj.common.dal.model.stream.KafkaStreamDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ProviderLiterals
import com.bwsw.sj.engine.core.testutils.benchmark.regular.SjBenchmarkPreparation

/**
  * It is needed to upload SJ entities such as providers, services, streams, a module and an instance
  *
  * @author Pavel Tomskikh
  */
class SjKafkaReaderBenchmarkPreparation(mongoPort: Int,
                                        zooKeeperHost: String,
                                        zooKeeperPort: Int,
                                        module: File,
                                        kafkaAddress: String,
                                        kafkaTopic: String,
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

  val kafkaProvider: ProviderDomain = new ProviderDomain(
    name = "benchmark-kafka-provider",
    description = "Kafka provider for benchmark",
    hosts = Array(kafkaAddress),
    providerType = ProviderLiterals.kafkaType,
    creationDate = new Date())

  val kafkaService: KafkaServiceDomain = new KafkaServiceDomain(
    name = "benchmark-kafka-service",
    description = "Kafka service for benchmark",
    provider = kafkaProvider,
    zkProvider = zooKeeperProvider,
    zkNamespace = zkNamespace,
    creationDate = new Date())

  override val inputStream: KafkaStreamDomain = new KafkaStreamDomain(
    name = kafkaTopic,
    service = kafkaService,
    partitions = 1,
    replicationFactor = 1,
    creationDate = new Date())

  override val inputStreamPartitions: Int = inputStream.partitions

  override protected def loadSpecificMetadata(connectionRepository: ConnectionRepository): Unit = {
    val providerRepository = connectionRepository.getProviderRepository
    val serviceRepository = connectionRepository.getServiceRepository
    val streamRepository = connectionRepository.getStreamRepository

    providerRepository.save(kafkaProvider)
    serviceRepository.save(kafkaService)
    streamRepository.save(inputStream)
  }
}
