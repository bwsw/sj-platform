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
package com.bwsw.sj.engine.regular.benchmark.read_tstream

import java.io.File
import java.util.UUID

import com.bwsw.common.embedded.EmbeddedMongo
import com.bwsw.sj.common.MongoAuthChecker
import com.bwsw.sj.common.config.TempHelperForConfigSetup
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.CommonAppConfigNames
import com.bwsw.sj.common.utils.benchmark.{ClassRunner, TStreamDataSender}
import com.bwsw.sj.engine.regular.RegularTaskRunner
import com.bwsw.sj.engine.regular.benchmark.ReaderBenchmark
import com.bwsw.sj.engine.regular.benchmark.utils.BenchmarkUtils.{findFreePort, retrieveResultFromFile}
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}
import com.typesafe.config.ConfigFactory

/**
  * Provides methods for testing the speed of reading data from T-Streams by SJ.
  *
  * @param zkHost        ZooKeeper server's host
  * @param zkPort        ZooKeeper server's port
  * @param tStreamToken  authentication token
  * @param tStreamPrefix ZooKeeper root node which holds coordination tree
  * @param words         list of words that are sent to the kafka server
  * @author Pavel Tomskikh
  */
class TStreamReaderBenchmark(zkHost: String,
                             zkPort: Int,
                             tStreamToken: String,
                             tStreamPrefix: String,
                             words: Array[String])
  extends ReaderBenchmark {
  private val zooKeeperAddress = zkHost + ":" + zkPort
  private val streamName: String = "performance-benchmark-" + UUID.randomUUID().toString
  private val tStreamDataSender: TStreamDataSender = new TStreamDataSender(
    zooKeeperAddress,
    streamName,
    tStreamToken,
    tStreamPrefix,
    words,
    " ")

  private val lookupResultTimeout: Long = 5000
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

  private val benchmarkPreparation = new TStreamReaderBenchmarkPreparation(
    mongoPort = mongoPort,
    zooKeeperHost = zkHost,
    zooKeeperPort = zkPort,
    module = module,
    streamName = streamName,
    zkNamespace = "benchmark",
    tStreamPrefix = tStreamPrefix,
    tStreamToken = tStreamToken,
    instanceName,
    taskName)

  private val tStreamsFactory = new TStreamsFactory
  tStreamsFactory.setProperty(ConfigurationOptions.Coordination.endpoints, zooKeeperAddress)
  tStreamsFactory.setProperty(ConfigurationOptions.Common.authenticationKey, tStreamToken)
  tStreamsFactory.setProperty(ConfigurationOptions.Stream.partitionsCount, 1)
  tStreamsFactory.setProperty(ConfigurationOptions.Coordination.path, tStreamPrefix)

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
    * Sends data into the Kafka server and runs an application under test
    *
    * @param messageSize   size of one message that is sent to the T-Streams server
    * @param messagesCount count of messages
    * @return time in milliseconds within which an application under test reads messages from Kafka
    */
  def runTest(messageSize: Long, messagesCount: Long): Long = {
    println(s"$messagesCount messages of $messageSize bytes")

    tStreamDataSender.send(messageSize, messagesCount)
    println("Data sent to the TStreams")

    benchmarkPreparation.loadInstance(outputFile.getAbsolutePath, messagesCount, connectionRepository.getInstanceRepository)
    val process: Process = new ClassRunner(classOf[RegularTaskRunner], environment = environment).start()

    var maybeResult: Option[Long] = retrieveResult(messageSize, messagesCount)
    while (maybeResult.isEmpty) {
      Thread.sleep(lookupResultTimeout)
      maybeResult = retrieveResult(messageSize, messagesCount)
    }

    process.destroy()

    client.deleteStream(benchmarkPreparation.inputStream.name)

    maybeResult.get
  }

  /**
    * Closes opened connections, deletes temporary files
    */
  def close(): Unit = {
    tStreamDataSender.close()
    tStreamsFactory.close()
  }

  /**
    * Is invoked every [[lookupResultTimeout]] milliseconds until result retrieved.
    * A result is a time in milliseconds within which an application under test reads messages from Kafka.
    *
    * @param messageSize   size of one message that is sent to the Kafka server
    * @param messagesCount count of messages
    * @return result if a test has done or None otherwise
    */
  private def retrieveResult(messageSize: Long, messagesCount: Long): Option[Long] =
    retrieveResultFromFile(outputFile).map(_.toLong)
}
