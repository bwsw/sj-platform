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
package com.bwsw.sj.engine.regular.module

import java.net.ServerSocket

import com.bwsw.common.embedded.{EmbeddedKafka, EmbeddedMongo}
import com.bwsw.sj.common.utils.NetworkUtils.findFreePort
import com.bwsw.sj.common.utils.benchmark.ClassRunner
import com.bwsw.sj.engine.core.testutils.Server
import com.bwsw.sj.engine.regular.module.SjRegularBenchmarkConstants._
import com.bwsw.sj.engine.regular.module.checkers.{SjRegularModuleStatefulBothChecker, SjRegularModuleStatefulKafkaChecker, SjRegularModuleStatefulTstreamChecker}
import org.apache.curator.test.TestingServer
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers, Outcome}

import scala.collection.mutable
import scala.util.Try

/**
  * Checks that regular-streaming module works properly
  *
  * @author Pavel Tomskikh
  */
class SjRegularModuleBenchmark extends FlatSpec with Matchers with TableDrivenPropertyChecks {
  val waitingTimeout = 1000
  val tStreamWriteTimeout = 60 * 1000
  val mongoPort = findFreePort()
  val ttsPort = findFreePort()
  val serverSocket = new ServerSocket(0)
  val localhost = "localhost"
  val agentsPorts = (0 until inputCount).map(_ => findFreePort())

  val environment = mutable.Map(
    "MONGO_HOSTS" -> s"$localhost:$mongoPort",
    "TSS_PORT" -> ttsPort.toString,
    "INSTANCE_NAME" -> "test-instance-for-regular-engine",
    "TASK_NAME" -> "test-instance-for-regular-engine-task0",
    "AGENTS_HOST" -> localhost,
    "AGENTS_PORTS" -> agentsPorts.mkString(","),
    "BENCHMARK_PORT" -> serverSocket.getLocalPort)

  val mongoServer = new EmbeddedMongo(mongoPort)

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

  "Regular-streaming module" should "work properly for T-Streams input streams" in {
    environment += "INPUT_STREAM_TYPES" -> tStreamMode

    val setup = runClass(classOf[SjRegularModuleSetup])
    setup.waitFor() shouldBe 0

    val waitResponseFromRunner = new Thread(() => serverSocket.accept())
    waitResponseFromRunner.start()

    var runner = runClass(classOf[SjRegularModuleRunner])

    while (waitResponseFromRunner.isAlive) {
      if (runner.isAlive)
        Thread.sleep(waitingTimeout)
      else
        runner = runClass(classOf[SjRegularModuleRunner])
    }

    Thread.sleep(tStreamWriteTimeout)
    runner.destroy()

    val checker = runClass(classOf[SjRegularModuleStatefulTstreamChecker])
    checker.waitFor() shouldBe 0
  }

  it should "work properly for Kafka input streams" in {
    environment += "INPUT_STREAM_TYPES" -> kafkaMode

    val setup = runClass(classOf[SjRegularModuleSetup])
    setup.waitFor() shouldBe 0

    val waitResponseFromRunner = new Thread(() => serverSocket.accept())
    waitResponseFromRunner.start()

    var runner = runClass(classOf[SjRegularModuleRunner])

    while (waitResponseFromRunner.isAlive) {
      if (runner.isAlive)
        Thread.sleep(waitingTimeout)
      else
        runner = runClass(classOf[SjRegularModuleRunner])
    }

    Thread.sleep(tStreamWriteTimeout)
    runner.destroy()

    val checker = runClass(classOf[SjRegularModuleStatefulKafkaChecker])
    checker.waitFor() shouldBe 0
  }

  it should "work properly for T-Streams and Kafka input streams" in {
    environment += "INPUT_STREAM_TYPES" -> commonMode

    val setup = runClass(classOf[SjRegularModuleSetup])
    setup.waitFor() shouldBe 0

    val waitResponseFromRunner = new Thread(() => serverSocket.accept())
    waitResponseFromRunner.start()

    var runner = runClass(classOf[SjRegularModuleRunner])

    while (waitResponseFromRunner.isAlive) {
      if (runner.isAlive)
        Thread.sleep(waitingTimeout)
      else
        runner = runClass(classOf[SjRegularModuleRunner])
    }

    Thread.sleep(tStreamWriteTimeout)
    runner.destroy()

    val checker = runClass(classOf[SjRegularModuleStatefulBothChecker])
    checker.waitFor() shouldBe 0
  }

  def runClass(clazz: Class[_]): Process =
    new ClassRunner(clazz, environment = environment.toMap).start()
}
