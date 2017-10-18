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
import java.util.UUID

import com.bwsw.common.embedded.EmbeddedMongo
import com.bwsw.sj.common.MongoAuthChecker
import com.bwsw.sj.common.config.TempHelperForConfigSetup
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.CommonAppConfigNames
import com.bwsw.sj.common.utils.benchmark.{ClassRunner, TStreamsDataSender}
import com.bwsw.sj.engine.core.testutils.benchmark.regular.RegularReaderBenchmark
import com.bwsw.sj.engine.regular.RegularTaskRunner
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}
import com.typesafe.config.ConfigFactory

/**
  * Provides methods for testing the speed of reading data from T-Streams by SJ.
  *
  * @param zkHost         ZooKeeper server's host
  * @param zkPort         ZooKeeper server's port
  * @param tStreamsToken  authentication token
  * @param tStreamsPrefix ZooKeeper root node which holds coordination tree
  * @param words          list of words that are sent to the kafka server
  * @author Pavel Tomskikh
  */
class TStreamsReaderBenchmark(zkHost: String,
                              zkPort: Int,
                              tStreamsToken: String,
                              tStreamsPrefix: String,
                              words: Array[String])
  extends RegularReaderBenchmark {
  private val zooKeeperAddress = zkHost + ":" + zkPort
  private val streamName: String = "performance-benchmark-" + UUID.randomUUID().toString
  private val tStreamsDataSender: TStreamsDataSender = new TStreamsDataSender(
    zooKeeperAddress,
    streamName,
    tStreamsToken,
    tStreamsPrefix,
    words,
    " ")

  private val moduleFilename = "../../contrib/benchmarks/sj-regular-performance-benchmark/target/scala-2.12/" +
    "sj-regular-performance-benchmark-1.0-SNAPSHOT.jar"

  private val module = new File(moduleFilename)

  private val mongoPort = findFreePort()
  private val mongoServer = new EmbeddedMongo(mongoPort)
  private val instanceName = "sj-benchmark-instance"
  private val taskName = instanceName + "-task"

  private val mongoAddress = "localhost:" + mongoPort
  private val config = ConfigFactory.load()
  private val mongoDatabase = config.getString(CommonAppConfigNames.mongoDbName)
  private val mongoAuthChecker = new MongoAuthChecker(mongoAddress, mongoDatabase)
  private lazy val connectionRepository = new ConnectionRepository(mongoAuthChecker, mongoAddress, mongoDatabase, None, None)

  private val benchmarkPreparation = new TStreamsReaderBenchmarkPreparation(
    mongoPort = mongoPort,
    zooKeeperHost = zkHost,
    zooKeeperPort = zkPort,
    module = module,
    streamName = streamName,
    zkNamespace = "benchmark",
    tStreamsPrefix = tStreamsPrefix,
    tStreamsToken = tStreamsToken,
    instanceName,
    taskName)

  private val tStreamsFactory = new TStreamsFactory
  tStreamsFactory.setProperty(ConfigurationOptions.Coordination.endpoints, zooKeeperAddress)
  tStreamsFactory.setProperty(ConfigurationOptions.Common.authenticationKey, tStreamsToken)
  tStreamsFactory.setProperty(ConfigurationOptions.Stream.partitionsCount, 1)
  tStreamsFactory.setProperty(ConfigurationOptions.Coordination.path, tStreamsPrefix)

  private val client = tStreamsFactory.getStorageClient()

  private val environment: Map[String, String] = Map(
    "ZOOKEEPER_HOST" -> zkHost,
    "ZOOKEEPER_PORT" -> zkPort.toString,
    "MONGO_HOSTS" -> mongoAddress,
    "INSTANCE_NAME" -> instanceName,
    "TASK_NAME" -> taskName,
    "AGENTS_HOST" -> "localhost",
    "AGENTS_PORTS" -> findFreePort().toString)

  /**
    * Starts mongo server
    */
  def startServices(): Unit = {
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


  /**
    * Generates data and send it to a storage
    *
    * @param messageSize     size of one message
    * @param messagesCount   count of messages
    * @param transactionSize count of messages per transaction
    */
  def sendData(messageSize: Long, messagesCount: Long, transactionSize: Long): Unit =
    tStreamsDataSender.send(messageSize, messagesCount, transactionSize)

  override def clearStorage(): Unit = {
    client.deleteStream(benchmarkPreparation.inputStream.name)
  }

  /**
    * Closes opened connections, deletes temporary files
    */
  override def stop(): Unit = {
    tStreamsDataSender.close()
    tStreamsFactory.close()
  }


  /**
    * Used to run an application under test in a separate process
    *
    * @param messagesCount count of messages
    * @return process of an application under test
    */
  override protected def runProcess(messagesCount: Long): Process = {
    benchmarkPreparation.loadInstance(outputFile.getAbsolutePath, messagesCount, connectionRepository.getInstanceRepository)

    new ClassRunner(classOf[RegularTaskRunner], environment = environment).start()
  }

  override def sendData(messageSize: Long, messagesCount: Long): Unit = {}
}
