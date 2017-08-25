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
import com.bwsw.sj.engine.batch.module.SjBatchModuleBenchmarkConstants._
import com.bwsw.sj.engine.batch.module.checkers.SjBatchModuleStatefulChecker
import com.bwsw.sj.engine.core.testutils.Server
import org.apache.curator.test.TestingServer
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers, Outcome}

import scala.util.Try

/**
  * @author Pavel Tomskikh
  */
class SjBatchModuleBenchmark extends FlatSpec with Matchers with MockitoSugar {
  val waitingTimeout = 1000
  val zkServer = new TestingServer(true)
  val zkAddress = zkServer.getConnectString
  val mongoPort = findFreePort()
  val ttsPort = findFreePort()
  val serverSocket = new ServerSocket(0)
  val kafkaServer = new EmbeddedKafka(Some(zkAddress))
  val mongoServer = new EmbeddedMongo(mongoPort)
  val localhost = "localhost"
  val agentsPorts = (0 until inputCount).map(_ => findFreePort())

  val environment = Map(
    "MONGO_HOSTS" -> s"$localhost:$mongoPort",
    "ZOOKEEPER_HOSTS" -> zkAddress,
    "TSS_PORT" -> ttsPort,
    "KAFKA_HOSTS" -> s"$localhost:${kafkaServer.port}",
    "INSTANCE_NAME" -> "test-instance-for-batch-engine",
    "TASK_NAME" -> "test-instance-for-batch-engine-task0",
    "AGENTS_HOST" -> localhost,
    "AGENTS_PORTS" -> agentsPorts.mkString(","),
    "BENCHMARK_PORT" -> serverSocket.getLocalPort)
    .mapValues(_.toString)


  override def withFixture(test: NoArgTest): Outcome = {
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

  "Batch-streaming module" should "work properly" in {
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

    Thread.sleep(waitingTimeout * 3)
    runner.destroy()

    val checker = runClass(classOf[SjBatchModuleStatefulChecker])
    checker.waitFor() shouldBe 0
  }

  def runClass(clazz: Class[_]): Process =
    new ClassRunner(clazz, environment = environment).start()
}
