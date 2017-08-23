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
package com.bwsw.sj.engine.input

import java.net.{ServerSocket, Socket}

import com.bwsw.common.embedded.EmbeddedMongo
import com.bwsw.sj.common.utils.NetworkUtils.findFreePort
import com.bwsw.sj.common.utils.benchmark.ClassRunner
import com.bwsw.sj.engine.core.testutils.Server
import com.bwsw.sj.engine.input.SjInputModuleBenchmarkConstants.instanceHost
import org.apache.curator.test.TestingServer
import org.scalatest.{FlatSpec, Matchers, Outcome}

import scala.util.Try

/**
  * @author Pavel Tomskikh
  */
class SjInputModuleBenchmark extends FlatSpec with Matchers {
  val waitingTimeout = 1000
  val zkServer = new TestingServer(true)
  val zkAddress = zkServer.getConnectString
  val mongoPort = findFreePort()
  val ttsPort = findFreePort()
  val serverSocket = new ServerSocket(0)
  val instancePort = findFreePort()

  val environment = Map(
    "MONGO_HOSTS" -> s"localhost:$mongoPort",
    "ZOOKEEPER_HOSTS" -> zkAddress,
    "TSS_PORT" -> ttsPort,
    "INSTANCE_NAME" -> "test-instance-for-input-engine",
    "TASK_NAME" -> "test-instance-for-input-engine-task0",
    "AGENTS_HOST" -> instanceHost,
    "ENTRY_PORT" -> instancePort,
    "INSTANCE_HOSTS" -> instanceHost,
    "BENCHMARK_PORT" -> serverSocket.getLocalPort)
    .mapValues(_.toString)

  val ttsServer = new ClassRunner(classOf[Server], environment = environment).start()
  Thread.sleep(waitingTimeout)

  val mongoServer = new EmbeddedMongo(mongoPort)
  mongoServer.start()

  override def withFixture(test: NoArgTest): Outcome = {
    val result = Try(super.withFixture(test))

    mongoServer.stop()
    ttsServer.destroy()
    zkServer.close()

    result.get
  }

  "Input-streaming module" should "work properly" in {
    val setup = new ClassRunner(classOf[SjInputModuleSetup], environment = environment).start()
    setup.waitFor() shouldBe 0

    val runner = new ClassRunner(classOf[SjInputModuleRunner], environment = environment).start()
    waitUntilModuleStarted()

    val dataWriter = new ClassRunner(classOf[SjInputModuleDataWriter], environment = environment).start()
    dataWriter.waitFor() shouldBe 0

    serverSocket.accept()
    serverSocket.close()
    Thread.sleep(waitingTimeout)
    runner.destroy()

    val duplicateChecker = new ClassRunner(classOf[SjInputModuleDuplicateCheckerRunner], environment = environment).start()
    duplicateChecker.waitFor() shouldBe 0
  }

  def waitUntilModuleStarted(): Unit = {
    while (Try(new Socket(instanceHost, instancePort)).isFailure)
      Thread.sleep(waitingTimeout)
  }
}
