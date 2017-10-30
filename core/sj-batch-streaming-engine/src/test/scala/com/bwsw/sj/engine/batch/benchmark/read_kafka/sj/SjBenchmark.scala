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

import java.io._

import com.bwsw.common.embedded.EmbeddedMongo
import com.bwsw.sj.common.MongoAuthChecker
import com.bwsw.sj.common.config.TempHelperForConfigSetup
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{CommonAppConfigNames, NetworkUtils}
import com.bwsw.sj.common.utils.benchmark.ClassRunner
import com.bwsw.sj.engine.batch.BatchTaskRunner
import com.bwsw.sj.engine.core.testutils.benchmark.batch.{BatchBenchmark, BatchBenchmarkConfig, BatchBenchmarkParameters}
import com.bwsw.sj.engine.core.testutils.benchmark.loader.kafka.KafkaBenchmarkDataSenderConfig
import com.bwsw.sj.engine.core.testutils.{Server, TestStorageServer}
import com.typesafe.config.ConfigFactory

/**
  * Provides methods for testing the speed of reading data from Kafka by Stream Juggler.
  *
  * Topic deletion must be enabled on the Kafka server.
  *
  * Host and port must point to the ZooKeeper server that used by the Kafka server.
  *
  * @param benchmarkConfig configuration of application
  * @param senderConfig    configuration of Kafka topic
  * @author Pavel Tomskikh
  */
class SjBenchmark(benchmarkConfig: BatchBenchmarkConfig,
                  senderConfig: KafkaBenchmarkDataSenderConfig)
  extends BatchBenchmark(benchmarkConfig) {

  private val zkAddressSplit = senderConfig.zooKeeperAddress.split(",")
  private val zkHost = zkAddressSplit(0)
  private val zkPort = zkAddressSplit(1).toInt

  private val moduleFilename = "../../contrib/benchmarks/sj-batch-performance-benchmark/target/scala-2.12/" +
    "sj-batch-performance-benchmark-1.0-SNAPSHOT.jar"
  private val module = new File(moduleFilename)

  private val mongoPort = NetworkUtils.findFreePort()
  private val mongoServer = new EmbeddedMongo(mongoPort)
  private val instanceName = "sj-benchmark-instance"
  private val taskName = instanceName + "-task"

  private val mongoAddress = "localhost:" + mongoPort
  private val config = ConfigFactory.load()
  private val mongoDatabase = config.getString(CommonAppConfigNames.mongoDbName)
  private val mongoAuthChecker = new MongoAuthChecker(mongoAddress, mongoDatabase)
  private lazy val connectionRepository =
    new ConnectionRepository(mongoAuthChecker, mongoAddress, mongoDatabase, None, None)

  private val benchmarkPreparation = new SjBenchmarkPreparation(
    mongoPort = mongoPort,
    zooKeeperHost = zkHost,
    zooKeeperPort = zkPort,
    module = module,
    kafkaAddress = senderConfig.kafkaAddress,
    kafkaTopic = senderConfig.topic,
    zkNamespace = "benchmark",
    tStreamPrefix = TestStorageServer.defaultPrefix,
    tStreamToken = TestStorageServer.defaultToken,
    instanceName,
    taskName)

  private var maybeTssProcess: Option[Process] = None

  private val environment: Map[String, String] = Map(
    "ZOOKEEPER_HOST" -> zkHost,
    "ZOOKEEPER_PORT" -> zkPort.toString,
    "MONGO_HOSTS" -> mongoAddress,
    "INSTANCE_NAME" -> instanceName,
    "TASK_NAME" -> taskName,
    "AGENTS_HOST" -> "localhost")


  /**
    * Starts [[com.bwsw.sj.engine.core.testutils.TestStorageServer]] and mongo servers
    */
  private def startServices(): Unit = {
    mongoServer.start()
    println("Mongo server started")
  }

  /**
    * Upload data in a mongo storage
    */
  override def prepare(): Unit = {
    startServices()

    val tempHelperForConfigSetup = new TempHelperForConfigSetup(connectionRepository)
    tempHelperForConfigSetup.setupConfigs(lowWatermark = 10000)
    println("Config settings loaded")

    benchmarkPreparation.prepare(connectionRepository)
    println("Entities loaded")
  }

  override def stop(): Unit = {
    maybeTssProcess.foreach(_.destroy())
    println("TSS server stopped")

    mongoServer.stop()
    println("Mongo server stopped")

    super.stop()
  }

  override protected def runProcess(parameters: BatchBenchmarkParameters, messagesCount: Long): Process = {
    maybeTssProcess.foreach(_.destroy())
    startTssProcess()

    benchmarkPreparation.loadInstance(
      outputFile.getAbsolutePath,
      messagesCount,
      parameters.batchSize,
      parameters.windowSize,
      parameters.slidingInterval,
      connectionRepository.getInstanceRepository)

    new ClassRunner(classOf[BatchTaskRunner], environment = environment).start()
  }


  private def startTssProcess(): Unit = {
    val tssEnv = Map(
      "ZOOKEEPER_HOSTS" -> senderConfig.zooKeeperAddress,
      "TSS_PORT" -> NetworkUtils.findFreePort())

    maybeTssProcess = Some(new ClassRunner(classOf[Server], environment = tssEnv).start())
    Thread.sleep(10000)
    println("TSS server started")
  }
}
