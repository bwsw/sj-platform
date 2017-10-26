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
package com.bwsw.sj.engine.batch.benchmark.read_kafka.sj

import java.io.File
import java.util.Date

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.benchmark.BatchExecutorOptions
import com.bwsw.sj.common.dal.model.instance._
import com.bwsw.sj.common.dal.model.module.BatchSpecificationDomain
import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.model.service.{KafkaServiceDomain, TStreamServiceDomain, ZKServiceDomain}
import com.bwsw.sj.common.dal.model.stream.{KafkaStreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.utils.{EngineLiterals, MessageResourceUtils, ProviderLiterals, SpecificationUtils}
import scaldi.Module

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * It is needed to upload SJ entities such as providers, services, streams, a module and an instance
  *
  * @author Pavel Tomskikh
  */
class SjBenchmarkPreparation(mongoPort: Int,
                             zooKeeperHost: String,
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
    providerType = ProviderLiterals.kafkaType,
    creationDate = new Date())

  val zooKeeperProvider: ProviderDomain = new ProviderDomain(
    name = "benchmark-zk-provider",
    description = "ZooKeeper provider for benchmark",
    hosts = Array(zooKeeperHost + ":" + zooKeeperPort),
    providerType = ProviderLiterals.zookeeperType,
    creationDate = new Date())

  val kafkaService: KafkaServiceDomain = new KafkaServiceDomain(
    name = "benchmark-kafka-service",
    description = "Kafka service for benchmark",
    provider = kafkaProvider,
    zkProvider = zooKeeperProvider,
    zkNamespace = zkNamespace,
    creationDate = new Date())

  val zooKeeperService: ZKServiceDomain = new ZKServiceDomain(
    name = "benchmark-zk-service",
    description = "ZooKeeper service for benchmark",
    provider = zooKeeperProvider,
    namespace = zkNamespace,
    creationDate = new Date())

  val tStreamService: TStreamServiceDomain = new TStreamServiceDomain(
    name = "benchmark-tstream-service",
    description = "TStream service for benchmark",
    provider = zooKeeperProvider,
    prefix = tStreamPrefix,
    token = tStreamToken,
    creationDate = new Date())

  val kafkaStream: KafkaStreamDomain = new KafkaStreamDomain(
    name = kafkaTopic,
    service = kafkaService,
    partitions = 1,
    replicationFactor = 1,
    creationDate = new Date())

  val tStreamStream = new TStreamStreamDomain(
    name = "benchmark-tstream-stream",
    service = tStreamService,
    partitions = 1,
    creationDate = new Date())

  private var maybeInstance: Option[BatchInstanceDomain] = None

  def prepare(connectionRepository: ConnectionRepository) = {
    loadMetadata(connectionRepository)
    val specification = loadModule(module, connectionRepository)
    maybeInstance = Some(createInstance(specification))
  }

  def loadInstance(outputFile: String,
                   messagesCount: Long,
                   batchSize: Long,
                   windowSize: Int,
                   slidingInterval: Int,
                   instanceRepository: GenericMongoRepository[InstanceDomain]): Unit = {
    tStreamStream.create()

    Try(instanceRepository.delete(instanceName))
    maybeInstance = Some(updateInstance(
      maybeInstance.get,
      outputFile,
      messagesCount,
      batchSize,
      windowSize,
      slidingInterval))

    instanceRepository.save(maybeInstance.get)
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
  }

  private def loadModule(module: File, connectionRepository: ConnectionRepository): BatchSpecificationDomain = {
    val specificationUtils = new SpecificationUtils
    val serializedSpecification = specificationUtils.getSpecificationFromJar(module)
    val specificationApi = jsonSerializer.deserialize[BatchSpecificationApi](serializedSpecification)
    val specification = specificationApi.to(new Module {
      bind[ConnectionRepository] to connectionRepository
      bind[MessageResourceUtils] to new MessageResourceUtils
    }).to

    val fileStorage = connectionRepository.getFileStorage
    fileStorage.put(module, module.getName, specification, "module")

    specification
  }

  private def createInstance(specification: BatchSpecificationDomain): BatchInstanceDomain = {
    val task = new Task()
    task.inputs.put(kafkaStream.name, Array(0, kafkaStream.partitions - 1))

    new BatchInstanceDomain(
      name = instanceName,
      moduleType = EngineLiterals.batchStreamingType,
      moduleName = specification.name,
      moduleVersion = specification.version,
      engine = specification.engineName + "-" + specification.engineVersion,
      coordinationService = zooKeeperService,
      status = EngineLiterals.started,
      inputs = Array(kafkaStream.name + "/split"),
      outputs = Array(tStreamStream.name),
      parallelism = 1,
      eventWaitIdleTime = 1,
      startFrom = EngineLiterals.oldestStartMode,
      executionPlan = new ExecutionPlan(Map(taskName -> task).asJava),
      performanceReportingInterval = Long.MaxValue,
      stateManagement = EngineLiterals.noneStateMode,
      stateFullCheckpoint = 0,
      creationDate = new Date())
  }

  private def updateInstance(instance: BatchInstanceDomain,
                             outputFile: String,
                             messagesCount: Long,
                             batchSize: Long,
                             windowSize: Int,
                             slidingInterval: Int): BatchInstanceDomain = {
    val options = BatchExecutorOptions(outputFile, messagesCount, batchSize)

    new BatchInstanceDomain(
      name = instance.name,
      moduleType = instance.moduleType,
      moduleName = instance.moduleName,
      moduleVersion = instance.moduleVersion,
      engine = instance.engine,
      coordinationService = instance.coordinationService,
      status = instance.status,
      restAddress = instance.restAddress,
      description = instance.description,
      parallelism = instance.parallelism,
      options = jsonSerializer.serialize(options),
      perTaskCores = instance.perTaskCores,
      perTaskRam = instance.perTaskRam,
      jvmOptions = instance.jvmOptions,
      nodeAttributes = instance.nodeAttributes,
      environmentVariables = instance.environmentVariables,
      stage = instance.stage,
      performanceReportingInterval = instance.performanceReportingInterval,
      frameworkId = instance.frameworkId,
      inputs = instance.inputs,
      outputs = instance.outputs,
      executionPlan = instance.executionPlan,
      startFrom = instance.startFrom,
      stateManagement = instance.stateManagement,
      window = windowSize,
      slidingInterval = slidingInterval,
      stateFullCheckpoint = instance.stateFullCheckpoint,
      eventWaitIdleTime = instance.eventWaitIdleTime,
      creationDate = new Date())
  }
}
