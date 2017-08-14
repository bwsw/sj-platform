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

import java.io._
import java.util.UUID

import com.bwsw.common.embedded.EmbeddedMongo
import com.bwsw.sj.common.MongoAuthChecker
import com.bwsw.sj.common.config.TempHelperForConfigSetup
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.CommonAppConfigNames
import com.bwsw.sj.common.utils.benchmark.ClassRunner
import com.bwsw.sj.engine.core.testutils.{Server, TestStorageServer}
import com.bwsw.sj.engine.regular.RegularTaskRunner
import com.bwsw.sj.engine.regular.benchmark.read_kafka.KafkaReaderBenchmark
import com.bwsw.sj.engine.regular.benchmark.utils.BenchmarkUtils.{findFreePort, retrieveResultFromFile}
import com.typesafe.config.ConfigFactory

/**
  * Provides methods for testing the speed of reading data from Kafka by Stream Juggler.
  *
  * Topic deletion must be enabled on the Kafka server.
  *
  * Host and port must point to the ZooKeeper server that used by the Kafka server.
  *
  * @param zkHost       ZooKeeper server's host
  * @param zkPort       ZooKeeper server's port
  * @param kafkaAddress Kafka server's address
  * @param words        list of words that are sent to the Kafka server
  * @author Pavel Tomskikh
  */
class SjBenchmark(zkHost: String,
                  zkPort: Int,
                  kafkaAddress: String,
                  words: Array[String]) extends {
  private val zooKeeperAddress = zkHost + ":" + zkPort
} with KafkaReaderBenchmark(zooKeeperAddress, kafkaAddress, words) {

  private val moduleFilename = "../../contrib/benchmarks/sj-regular-performance-benchmark/target/scala-2.12/" +
    "sj-regular-performance-benchmark-1.0-SNAPSHOT.jar"
  private val module = new File(moduleFilename)

  private val mongoPort = findFreePort()
  private val mongoServer = new EmbeddedMongo(mongoPort)
  private val instanceName = "sj-benchmark-instance"
  private val taskName = instanceName + "-task"
  private val outputFilename = "benchmark-output-" + UUID.randomUUID().toString
  private val outputFile = new File(outputFilename)

  private val mongoAddress = "localhost:" + mongoPort
  private val config = ConfigFactory.load()
  private val mongoDatabase = config.getString(CommonAppConfigNames.mongoDbName)
  private val mongoAuthChecker = new MongoAuthChecker(mongoAddress, mongoDatabase)
  private lazy val connectionRepository = new ConnectionRepository(mongoAuthChecker, mongoAddress, mongoDatabase, None, None)

  private val benchmarkPreparation = new SjBenchmarkPreparation(
    mongoPort = mongoPort,
    zooKeeperHost = zkHost,
    zooKeeperPort = zkPort,
    module = module,
    kafkaAddress = kafkaAddress,
    kafkaTopic = kafkaTopic,
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
  def startServices(): Unit = {
    val tssEnv = Map("ZOOKEEPER_HOSTS" -> zooKeeperAddress, "TSS_PORT" -> findFreePort().toString)

    maybeTssProcess = Some(new ClassRunner(classOf[Server], environment = tssEnv).start())
    Thread.sleep(1000)
    println("TSS server started")

    mongoServer.start()
    println("Mongo server started")
  }

  /**
    * Upload data in a mongo storage
    */
  def prepare(): Unit = {
    val tempHelperForConfigSetup = new TempHelperForConfigSetup(connectionRepository)
    tempHelperForConfigSetup.setupConfigs()
    println("Config settings loaded")

    benchmarkPreparation.prepare(connectionRepository)
    println("Entities loaded")
  }

  override def close(): Unit = {
    maybeTssProcess.foreach(_.destroy())
    println("TSS server stopped")

    mongoServer.stop()
    println("Mongo server stopped")

    super.close()
  }


  override protected def runProcess(messageSize: Long, messagesCount: Long): Process = {
    benchmarkPreparation.loadInstance(outputFile.getAbsolutePath, messagesCount, connectionRepository.getInstanceRepository)

    new ClassRunner(classOf[RegularTaskRunner], environment = environment).start()
  }

  override protected def retrieveResult(messageSize: Long, messagesCount: Long): Option[Long] =
    retrieveResultFromFile(outputFile).map(_.toLong)
}
