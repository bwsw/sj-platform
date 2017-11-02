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
package com.bwsw.sj.engine.core.testutils.benchmark.sj

import java.io.File
import java.util.Date

import com.bwsw.common.JsonSerializer
import com.bwsw.common.embedded.EmbeddedMongo
import com.bwsw.sj.common.MongoAuthChecker
import com.bwsw.sj.common.config.TempHelperForConfigSetup
import com.bwsw.sj.common.dal.model.instance.{ExecutionPlan, InstanceDomain, Task}
import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.model.service.{TStreamServiceDomain, ZKServiceDomain}
import com.bwsw.sj.common.dal.model.stream.TStreamStreamDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.FileMetadataLiterals
import com.bwsw.sj.common.utils.benchmark.ClassRunner
import com.bwsw.sj.common.utils.{CommonAppConfigNames, NetworkUtils, ProviderLiterals}
import com.bwsw.sj.engine.core.testutils.Server
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._

/**
  * Provides methods for testing the speed of reading data from storage by Stream Juggler
  *
  * @author Pavel Tomskikh
  */
abstract class SjBenchmarkHelper[T <: InstanceDomain](zooKeeperAddress: String,
                                                      val zkNamespace: String,
                                                      tStreamsPrefix: String,
                                                      tStreamsToken: String,
                                                      instanceFactory: InstanceFactory[T],
                                                      inputStreamFactory: InputStreamFactory,
                                                      moduleFilename: String,
                                                      runEmbeddedTTS: Boolean) {

  protected val module: File = new File(moduleFilename)
  protected val mongoPort: Int = NetworkUtils.findFreePort()
  protected val splitZooKeeperAddress: Array[String] = zooKeeperAddress.split(":")
  protected val zooKeeperHost: String = splitZooKeeperAddress(0)
  protected val zooKeeperPort: Int = splitZooKeeperAddress(1).toInt
  protected val mongoAddress: String = "localhost:" + mongoPort
  protected val mongoServer: EmbeddedMongo = new EmbeddedMongo(mongoPort)
  protected val config: Config = ConfigFactory.load()
  protected val mongoDatabase: String = config.getString(CommonAppConfigNames.mongoDbName)
  protected val mongoAuthChecker: MongoAuthChecker = new MongoAuthChecker(mongoAddress, mongoDatabase)
  protected val instanceName: String = "sj-benchmark-instance"
  protected var taskName: String = instanceName + "-task0"
  protected val jsonSerializer = new JsonSerializer(ignoreUnknown = true)
  protected var maybeInstance: Option[T] = None
  protected var maybeTssProcess: Option[Process] = None
  protected lazy val connectionRepository: ConnectionRepository =
    new ConnectionRepository(mongoAuthChecker, mongoAddress, mongoDatabase, None, None)


  val zooKeeperProvider: ProviderDomain = new ProviderDomain(
    name = "benchmark-zk-provider",
    description = "ZooKeeper provider for benchmark",
    hosts = Array(zooKeeperAddress),
    providerType = ProviderLiterals.zookeeperType,
    creationDate = new Date())

  val zooKeeperService: ZKServiceDomain = new ZKServiceDomain(
    name = "benchmark-zk-service",
    description = "ZooKeeper service for benchmark",
    provider = zooKeeperProvider,
    namespace = zkNamespace,
    creationDate = new Date())

  val tStreamsService: TStreamServiceDomain = new TStreamServiceDomain(
    name = "benchmark-tstreams-service",
    description = "T-Streams service for benchmark",
    provider = zooKeeperProvider,
    prefix = tStreamsPrefix,
    token = tStreamsToken,
    creationDate = new Date())

  val outputStream = new TStreamStreamDomain(
    name = "benchmark-output-stream",
    service = tStreamsService,
    partitions = 1,
    creationDate = new Date())

  var environment: Map[String, Any] = Map(
    "ZOOKEEPER_HOST" -> zooKeeperHost,
    "ZOOKEEPER_PORT" -> zooKeeperPort,
    "MONGO_HOSTS" -> mongoAddress,
    "INSTANCE_NAME" -> instanceName,
    "TASK_NAME" -> taskName,
    "AGENTS_HOST" -> "localhost",
    "AGENTS_PORTS" -> NetworkUtils.findFreePort())

  def start(): Unit = {
    if (runEmbeddedTTS) startTssProcess()

    mongoServer.start()

    val tempHelperForConfigSetup = new TempHelperForConfigSetup(connectionRepository)
    tempHelperForConfigSetup.setupConfigs(lowWatermark = 10000)

    connectionRepository.getProviderRepository.save(zooKeeperProvider)
    connectionRepository.getServiceRepository.save(zooKeeperService)
    connectionRepository.getServiceRepository.save(tStreamsService)
    connectionRepository.getStreamRepository.save(outputStream)
    outputStream.create()

    val inputStream = inputStreamFactory.loadInputStream(this, connectionRepository)

    val task = new Task()
    task.inputs.put(inputStream.name, Array(0, inputStreamFactory.partitions - 1))
    val executionPlan = new ExecutionPlan(Map(taskName -> task).asJava)

    val (instance, specification) = instanceFactory.create(
      module,
      instanceName,
      zooKeeperService,
      inputStream,
      outputStream,
      executionPlan)

    connectionRepository.getFileStorage.put(module, module.getName, specification, FileMetadataLiterals.moduleType)
    connectionRepository.getInstanceRepository.save(instance)
    maybeInstance = Some(instance)
  }

  def stop(): Unit = {
    maybeTssProcess.foreach(_.destroy())
    maybeTssProcess = None
    mongoServer.stop()
  }


  protected def startTssProcess(): Unit = {
    val tssEnv = Map(
      "ZOOKEEPER_HOSTS" -> zooKeeperAddress,
      "TSS_PORT" -> NetworkUtils.findFreePort())

    maybeTssProcess = Some(new ClassRunner(classOf[Server], environment = tssEnv).start())
    Thread.sleep(10000)
    println("TSS server started")
  }
}
