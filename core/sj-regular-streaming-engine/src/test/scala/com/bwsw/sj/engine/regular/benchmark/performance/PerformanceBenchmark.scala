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

import java.io.{File, FileWriter}

import com.bwsw.common.embedded.EmbeddedMongo
import com.bwsw.sj.common.config.TempHelperForConfigSetup
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.engine.core.testutils.{Server, TestStorageServer}
import com.bwsw.sj.engine.regular.RegularTaskRunner
import com.bwsw.sj.kafka.data_sender.DataSender
import org.apache.curator.test.TestingServer
import scaldi.Injectable.inject
import scaldi.Injector

/**
  * @author Pavel Tomskikh
  */
class PerformanceBenchmark(mongoPort: Int,
                           zkPort: Int,
                           kafkaAddress: String,
                           kafkaTopic: String,
                           messagesCount: Long,
                           instanceName: String,
                           words: Array[String],
                           outputFileName: String)
                          (implicit injector: Injector) {

  private val outputFile = new File(outputFileName)
  private val moduleFilename = "./contrib/benchmarks/sj-regular-performance-benchmark/target/scala-2.12/" +
    "sj-regular-performance-benchmark-1.0-SNAPSHOT.jar"
  private val module = new File(moduleFilename)

  private val mongoServer = new EmbeddedMongo(mongoPort)
  private val zooKeeperServer = new TestingServer(zkPort, false)
  private val kafkaSender = new DataSender(kafkaAddress, kafkaTopic, words, " ")
  private val fileCheckTimeout = 5000
  private val taskName = instanceName + "-task"

  private val benchmarkPreparation = new BenchmarkPreparation(
    mongoPort = mongoPort,
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

  private val environment = Map(
    "MONGO_HOSTS" -> s"localhost:$mongoPort",
    "INSTANCE_NAME" -> instanceName,
    "TASK_NAME" -> taskName,
    "AGENTS_HOST" -> "localhost")

  def startServices(): Unit = {
    zooKeeperServer.start()
    println("ZooKeeper server started")

    val ttsEnv = Map("ZOOKEEPER_HOSTS" -> s"localhost:$zkPort")
    maybeTtsProcess = Some(new SeparateProcess(classOf[Server], ttsEnv).start())
    Thread.sleep(1000)
    println("TTS server starte")

    mongoServer.start()
    println("Mongo server started")
  }

  def prepare(): Unit = {
    TempHelperForConfigSetup.setupConfigs()
    println("Config settings loaded")

    benchmarkPreparation.prepare(outputFile.getAbsolutePath, messagesCount, inject[ConnectionRepository])
    println("Entities loaded")
  }

  def runTest(messageSize: Long): Unit = {
    println(s"$messageSize bytes messages")

    val writer = new FileWriter(outputFile, true)
    writer.write(messageSize + ",")
    writer.close()

    val lastModified = outputFile.lastModified()

    kafkaSender.send(messageSize, messagesCount)
    println("Data sent to the Kafka")

    val process = new SeparateProcess(classOf[RegularTaskRunner], environment).start()

    while (outputFile.lastModified() == lastModified)
      Thread.sleep(fileCheckTimeout)

    process.destroy()

    // todo: cleanup kafka
  }

  def stopServices() = {
    kafkaSender.close()

    maybeTtsProcess.foreach(_.destroy())

    zooKeeperServer.close()
    println("ZooKeeper server stopped")

    mongoServer.stop()
    println("Mongo server stopped")
  }
}
