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

import java.io._
import java.util.UUID

import com.bwsw.common.KafkaClient
import com.bwsw.common.embedded.EmbeddedMongo
import com.bwsw.sj.common.config.TempHelperForConfigSetup
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.benchmark.ClassRunner
import com.bwsw.sj.engine.core.testutils.{Server, TestStorageServer}
import com.bwsw.sj.engine.regular.RegularTaskRunner
import com.bwsw.sj.kafka.data_sender.DataSender
import scaldi.Injectable.inject
import scaldi.Injector

/**
  * Provides methods for testing speed of reading data from Kafka.
  *
  * Topic deletion must be enabled on the Kafka server.
  *
  * Host and port must point to the ZooKeeper server that used by the Kafka server.
  *
  * @param mongoPort    port for [[EmbeddedMongo]]
  * @param zkHost       ZooKeeper server's host
  * @param zkPort       ZooKeeper server's port
  * @param kafkaAddress Kafka server's address
  * @param instanceName instance's name
  * @param words        list of words that sends to the kafka server
  * @author Pavel Tomskikh
  */
class PerformanceBenchmark(mongoPort: Int,
                           zkHost: String,
                           zkPort: Int,
                           kafkaAddress: String,
                           instanceName: String,
                           words: Array[String])
                          (implicit injector: Injector) {

  private val kafkaTopic = "performance-benchmark-test-" + UUID.randomUUID().toString
  private val moduleFilename = "../../contrib/benchmarks/sj-regular-performance-benchmark/target/scala-2.12/" +
    "sj-regular-performance-benchmark-1.0-SNAPSHOT.jar"
  // for run from IDEA
  //private val moduleFilename = "./contrib/benchmarks/sj-regular-performance-benchmark/target/scala-2.12/" +
  //"sj-regular-performance-benchmark-1.0-SNAPSHOT.jar"
  private val module = new File(moduleFilename)

  private val zkAddress = s"$zkHost:$zkPort"
  private val mongoServer = new EmbeddedMongo(mongoPort)
  private val kafkaClient = new KafkaClient(Array(zkAddress))
  private val kafkaSender = new DataSender(kafkaAddress, kafkaTopic, words, " ")
  private val lookupResultTimeout = 5000
  private val taskName = instanceName + "-task"
  private val connectionRepository = inject[ConnectionRepository]

  private val benchmarkPreparation = new BenchmarkPreparation(
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

  private var maybeTtsProcess: Option[Process] = None

  private val environment: Map[String, String] = Map(
    "ZOOKEEPER_HOST" -> zkHost,
    "ZOOKEEPER_PORT" -> zkPort.toString,
    "MONGO_HOSTS" -> s"localhost:$mongoPort",
    "INSTANCE_NAME" -> instanceName,
    "TASK_NAME" -> taskName,
    "AGENTS_HOST" -> "localhost")


  /**
    * Starts tts and mongo servers
    */
  def startServices(): Unit = {
    val ttsEnv = Map("ZOOKEEPER_HOSTS" -> zkAddress)
    maybeTtsProcess = Some(new ClassRunner(classOf[Server], ttsEnv).start())
    Thread.sleep(1000)
    println("TTS server started")

    mongoServer.start()
    println("Mongo server started")
  }

  /**
    * Upload data in a mongo storage
    */
  def prepare(): Unit = {
    TempHelperForConfigSetup.setupConfigs()
    println("Config settings loaded")

    benchmarkPreparation.prepare(connectionRepository)
    println("Entities loaded")
  }

  /**
    * Performs first test because it's need more time than next
    */
  def warmUp(): Long = {
    runTest(10, 10)
  }

  /**
    * Sends data into the Kafka server and runs module
    *
    * @param messageSize   size of one message that sends to the Kafka server
    * @param messagesCount count of messages
    */
  def runTest(messageSize: Long, messagesCount: Long): Long = {
    println(s"$messageSize bytes messages")
    val outputFilename = "benchmark-output-" + UUID.randomUUID().toString
    val outputFile = new File(outputFilename)

    benchmarkPreparation.loadInstance(outputFile.getAbsolutePath, messagesCount, connectionRepository.getInstanceRepository)

    val lastModified = outputFile.lastModified()

    kafkaClient.createTopic(kafkaTopic, 1, 1)
    while (!kafkaClient.topicExists(kafkaTopic))
      Thread.sleep(100)

    println(s"Kafka topic $kafkaTopic created")

    kafkaSender.send(messageSize, messagesCount)
    println("Data sent to the Kafka")

    val process = new ClassRunner(classOf[RegularTaskRunner], environment).start()

    while (outputFile.lastModified() == lastModified)
      Thread.sleep(lookupResultTimeout)

    kafkaClient.deleteTopic(kafkaTopic)
    process.destroy()

    while (kafkaClient.topicExists(kafkaTopic))
      Thread.sleep(100)

    println(s"Kafka topic $kafkaTopic deleted")

    val reader = new BufferedReader(new FileReader(outputFile))
    val result = reader.readLine().toLong
    reader.close()
    outputFile.delete()

    result
  }

  /**
    * Stops tts and mongo servers
    */
  def stopServices() = {
    kafkaClient.close()
    maybeTtsProcess.foreach(_.destroy())
    println("TTS server stopped")

    mongoServer.stop()
    println("Mongo server stopped")
  }
}
