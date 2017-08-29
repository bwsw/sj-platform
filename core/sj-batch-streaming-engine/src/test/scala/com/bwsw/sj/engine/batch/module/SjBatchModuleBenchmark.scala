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
package com.bwsw.sj.engine.batch.module

import java.net.ServerSocket

import com.bwsw.common.embedded.{EmbeddedKafka, EmbeddedMongo}
import com.bwsw.sj.common.utils.NetworkUtils.findFreePort
import com.bwsw.sj.common.utils.benchmark.ClassRunner
import com.bwsw.sj.engine.batch.module.SjBatchModuleBenchmarkConstants.{commonMode, inputCount, kafkaMode, tStreamMode}
import com.bwsw.sj.engine.batch.module.checkers.{SjBatchModuleStatefulBothChecker, SjBatchModuleStatefulKafkaChecker, SjBatchModuleStatefulTstreamChecker}
import com.bwsw.sj.engine.core.testutils.Server
import org.apache.curator.test.TestingServer
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers, Outcome}

import scala.collection.mutable
import scala.util.Try

/**
  * @author Pavel Tomskikh
  */
class SjBatchModuleBenchmark extends FlatSpec with Matchers with TableDrivenPropertyChecks {
  val waitingTimeout = 1000
  val tStreamWritingTimeout = 30 * 1000
  val mongoPort = findFreePort()
  val ttsPort = findFreePort()
  val serverSocket = new ServerSocket(0)
  val localhost = "localhost"
  val agentsPorts = (0 until inputCount).map(_ => findFreePort())

  val environment = mutable.Map(
    "MONGO_HOSTS" -> s"$localhost:$mongoPort",
    "TSS_PORT" -> ttsPort.toString,
    "INSTANCE_NAME" -> "test-instance-for-batch-engine",
    "TASK_NAME" -> "test-instance-for-batch-engine-task0",
    "AGENTS_HOST" -> localhost,
    "AGENTS_PORTS" -> agentsPorts.mkString(","),
    "BENCHMARK_PORT" -> serverSocket.getLocalPort.toString)


  override def withFixture(test: NoArgTest): Outcome = {
    val zkServer = new TestingServer(true)
    val zkAddress = zkServer.getConnectString
    environment += "ZOOKEEPER_HOSTS" -> zkAddress

    val kafkaServer = new EmbeddedKafka(Some(zkAddress))
    environment += "KAFKA_HOSTS" -> s"$localhost:${kafkaServer.port}"

    val mongoServer = new EmbeddedMongo(mongoPort)

    val ttsServer = runClass(classOf[Server])
    Thread.sleep(waitingTimeout)

    mongoServer.start()
    kafkaServer.start()

    val result = Try(super.withFixture(test))

    mongoServer.stop()
    ttsServer.destroy()
    kafkaServer.stop()
    zkServer.close()

    result.get
  }

  "Batch-streaming module" should "work properly for T-Streams input streams" in {
    environment += "INPUT_STREAM_TYPES" -> tStreamMode

    val setup = runClass(classOf[SjBatchModuleSetup])
    setup.waitFor() shouldBe 0

    val waitResponseFromRunner = new Thread(() => serverSocket.accept())
    waitResponseFromRunner.start()

    var runner = runClass(classOf[SjBatchModuleRunner])

    while (waitResponseFromRunner.isAlive) {
      if (runner.isAlive)
        Thread.sleep(waitingTimeout)
      else
        runner = runClass(classOf[SjBatchModuleRunner])
    }

    Thread.sleep(tStreamWritingTimeout)
    runner.destroy()

    val checker = runClass(classOf[SjBatchModuleStatefulTstreamChecker])
    checker.waitFor() shouldBe 0
  }

  it should "work properly for Kafka input streams" in {
    environment += "INPUT_STREAM_TYPES" -> kafkaMode

    val setup = runClass(classOf[SjBatchModuleSetup])
    setup.waitFor() shouldBe 0

    val waitResponseFromRunner = new Thread(() => serverSocket.accept())
    waitResponseFromRunner.start()

    var runner = runClass(classOf[SjBatchModuleRunner])

    while (waitResponseFromRunner.isAlive) {
      if (runner.isAlive)
        Thread.sleep(waitingTimeout)
      else
        runner = runClass(classOf[SjBatchModuleRunner])
    }

    Thread.sleep(tStreamWritingTimeout)
    runner.destroy()

    val checker = runClass(classOf[SjBatchModuleStatefulKafkaChecker])
    checker.waitFor() shouldBe 0
  }

  it should "work properly for T-Streams and Kafka input streams" in {
    environment += "INPUT_STREAM_TYPES" -> commonMode

    val setup = runClass(classOf[SjBatchModuleSetup])
    setup.waitFor() shouldBe 0

    val waitResponseFromRunner = new Thread(() => serverSocket.accept())
    waitResponseFromRunner.start()

    var runner = runClass(classOf[SjBatchModuleRunner])

    while (waitResponseFromRunner.isAlive) {
      if (runner.isAlive)
        Thread.sleep(waitingTimeout)
      else
        runner = runClass(classOf[SjBatchModuleRunner])
    }

    Thread.sleep(tStreamWritingTimeout)
    runner.destroy()

    val checker = runClass(classOf[SjBatchModuleStatefulBothChecker])
    checker.waitFor() shouldBe 0
  }

  def runClass(clazz: Class[_]): Process =
    new ClassRunner(clazz, environment = environment.toMap).start()
}
