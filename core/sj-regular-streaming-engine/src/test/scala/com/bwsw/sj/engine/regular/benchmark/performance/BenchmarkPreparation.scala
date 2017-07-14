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
package com.bwsw.sj.engine.regular.benchmark.performance

import java.io.File

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.benchmark.RegularExecutorOptions
import com.bwsw.sj.common.dal.model.instance.{ExecutionPlan, RegularInstanceDomain, Task}
import com.bwsw.sj.common.dal.model.module.SpecificationDomain
import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.model.service.{KafkaServiceDomain, TStreamServiceDomain, ZKServiceDomain}
import com.bwsw.sj.common.dal.model.stream.{KafkaStreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{EngineLiterals, ProviderLiterals, SpecificationUtils}

import scala.collection.JavaConverters._

/**
  * @author Pavel Tomskikh
  */
class BenchmarkPreparation(mongoPort: Int,
                           zooKeeperPort: Int,
                           module: File,
                           kafkaAddress: String,
                           kafkaTopic: String,
                           zkNamespace: String,
                           tStreamPrefix: String,
                           tStreamToken: String,
                           instanceName: String,
                           taskName: String) {

  private val jsonSerializer = new JsonSerializer(ignoreUnknown = true)

  val kafkaProvider: ProviderDomain = new ProviderDomain(
    name = "benchmark-kafka-provider",
    description = "Kafka provider for benchmark",
    hosts = Array(kafkaAddress),
    login = null,
    password = null,
    providerType = ProviderLiterals.kafkaType)

  val zooKeeperProvider: ProviderDomain = new ProviderDomain(
    name = "benchmark-zk-provider",
    description = "ZooKeeper provider for benchmark",
    hosts = Array("localhost:" + zooKeeperPort),
    login = null,
    password = null,
    providerType = ProviderLiterals.zookeeperType)

  val kafkaService: KafkaServiceDomain = new KafkaServiceDomain(
    name = "benchmark-kafka-service",
    description = "Kafka service for benchmark",
    provider = kafkaProvider,
    zkProvider = zooKeeperProvider,
    zkNamespace = zkNamespace)

  val zooKeeperService: ZKServiceDomain = new ZKServiceDomain(
    name = "benchmark-zk-service",
    description = "ZooKeeper service for benchmark",
    provider = zooKeeperProvider,
    namespace = zkNamespace)

  val tStreamService: TStreamServiceDomain = new TStreamServiceDomain(
    name = "benchmark-tstream-service",
    description = "TStream service for benchmark",
    provider = zooKeeperProvider,
    prefix = tStreamPrefix,
    token = tStreamToken)

  val kafkaStream: KafkaStreamDomain = new KafkaStreamDomain(
    name = kafkaTopic,
    service = kafkaService,
    partitions = 1,
    replicationFactor = 1)

  val tStreamStream = new TStreamStreamDomain(
    name = "benchmark-tstream-stream",
    service = tStreamService,
    partitions = 1)


  def prepare(outputFile: String, messagesCount: Long, connectionRepository: ConnectionRepository) = {
    loadMetadata(connectionRepository)
    val specification = loadModule(module, connectionRepository)
    loadInstance(outputFile, messagesCount, specification, connectionRepository)
  }


  private def loadMetadata(connectionRepository: ConnectionRepository): Unit = {
    val providerRepository = connectionRepository.getProviderRepository
    val serviceRepository = connectionRepository.getServiceRepository
    val streamRepository = connectionRepository.getStreamRepository

    providerRepository.save(zooKeeperProvider)
    providerRepository.save(kafkaProvider)
    serviceRepository.save(zooKeeperService)
    serviceRepository.save(kafkaService)
    serviceRepository.save(tStreamService)
    streamRepository.save(kafkaStream)
    streamRepository.save(tStreamStream)
    tStreamStream.create()
  }

  private def loadModule(module: File, connectionRepository: ConnectionRepository): SpecificationDomain = {
    val specificationUtils = new SpecificationUtils
    val serializedSpecification = specificationUtils.getSpecificationFromJar(module)
    val specification = jsonSerializer.deserialize[Map[String, Any]](serializedSpecification)
    val fileStorage = connectionRepository.getFileStorage
    fileStorage.put(module, module.getName, specification, "module")

    jsonSerializer.deserialize[SpecificationDomain](serializedSpecification)
  }

  private def loadInstance(outputFile: String,
                           messagesCount: Long,
                           specification: SpecificationDomain,
                           connectionRepository: ConnectionRepository): Unit = {
    val instance = createInstance(outputFile, messagesCount, specification)
    val instanceRepository = connectionRepository.getInstanceRepository

    instanceRepository.save(instance)
  }

  private def createInstance(outputFile: String,
                             messagesCount: Long,
                             specification: SpecificationDomain): RegularInstanceDomain = {
    val options = RegularExecutorOptions(outputFile, messagesCount)
    val task = new Task()
    task.inputs.put(kafkaStream.name, Array(0, kafkaStream.partitions - 1))

    new RegularInstanceDomain(
      name = instanceName,
      moduleType = EngineLiterals.regularStreamingType,
      moduleName = specification.name,
      moduleVersion = specification.version,
      engine = specification.engineName + "-" + specification.engineVersion,
      coordinationService = zooKeeperService,
      status = EngineLiterals.started,
      options = jsonSerializer.serialize(options),
      inputs = Array(kafkaStream.name + "/split"),
      outputs = Array(tStreamStream.name),
      eventWaitIdleTime = 1,
      checkpointMode = EngineLiterals.everyNthMode,
      startFrom = EngineLiterals.oldestStartMode,
      executionPlan = new ExecutionPlan(Map(taskName -> task).asJava))
  }
}
