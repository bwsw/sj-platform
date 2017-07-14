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
import com.bwsw.sj.engine.core.testutils.TestStorageServer
import com.bwsw.sj.engine.regular.RegularTaskRunner
import com.bwsw.sj.kafka.data_sender.DataSender
import org.apache.curator.test.TestingServer
import scaldi.Injectable.inject
import scaldi.Injector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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

  private val testStorageServer = new TestStorageServer(
    token = TestStorageServer.defaultToken,
    prefix = TestStorageServer.defaultPrefix,
    streamPath = TestStorageServer.defaultStreamPath,
    zkHosts = "localhost",
    host = "localhost")

  private val benchmarkPreparation = new BenchmarkPreparation(
    mongoPort = mongoPort,
    zooKeeperPort = zkPort,
    module = module,
    kafkaAddress = kafkaAddress,
    kafkaTopic = kafkaTopic,
    zkNamespace = "benchmark",
    tStreamPrefix = TestStorageServer.defaultPrefix,
    tStreamToken = TestStorageServer.defaultToken,
    instanceName)


  def prepare(): Unit = {
    mongoServer.start()
    println("Mongo server started")

    zooKeeperServer.start()
    println("ZooKeeper server started")

    Future(testStorageServer.start())

    TempHelperForConfigSetup.setupConfigs()
    println("Config settings loaded")

    benchmarkPreparation.prepare(outputFile.getAbsolutePath, messagesCount, inject[ConnectionRepository])
    println("Entities loaded")
  }

  def runTest(messageSize: Long): Unit = {
    val writer = new FileWriter(outputFile, true)
    writer.write(messageSize + ",")
    writer.close()

    val lastModified = outputFile.lastModified()

    kafkaSender.send(messageSize, messagesCount)
    println("Data sent to the Kafka")

    Future(RegularTaskRunner.main(Array()))

    while (outputFile.lastModified() == lastModified)
      Thread.sleep(fileCheckTimeout)

    // todo: stop regular task runner
    // todo: cleanup kafka
  }

  def stop() = {
    zooKeeperServer.close()
    println("ZooKeeper server stopped")

    mongoServer.stop()
    println("Mongo server stopped")
  }
}
