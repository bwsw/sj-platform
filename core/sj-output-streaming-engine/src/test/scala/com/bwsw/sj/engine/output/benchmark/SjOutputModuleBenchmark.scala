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
package com.bwsw.sj.engine.output.benchmark

import java.net.ServerSocket

import com.bwsw.common.embedded.{EmbeddedElasticsearch, EmbeddedMongo}
import com.bwsw.sj.common.utils.NetworkUtils.findFreePort
import com.bwsw.sj.common.utils.benchmark.ClassRunner
import com.bwsw.sj.engine.core.testutils.Server
import com.bwsw.sj.engine.output.benchmark.SjOutputModuleBenchmarkConstants._
import com.bwsw.sj.engine.output.benchmark.data_checkers.{ESDataChecker, JDBCDataChecker, RestDataChecker}
import org.apache.curator.test.TestingServer
import org.scalatest.{FlatSpec, Matchers, Outcome}
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres
import ru.yandex.qatools.embed.postgresql.distribution.Version

import scala.util.Try

/**
  * @author Pavel Tomskikh
  */
class SjOutputModuleBenchmark extends FlatSpec with Matchers {
  val waitingTimeout = 1000
  val zkPort = findFreePort()
  val mongoPort = findFreePort()
  val ttsPort = findFreePort()
  val restPort = findFreePort()
  val esPort = findFreePort()
  val jdbcPort = findFreePort()

  val serverSocket = new ServerSocket(0)
  val localhost = "localhost"
  val agentsPort = findFreePort()

  val commonEnvironment = Map(
    "MONGO_HOSTS" -> s"$localhost:$mongoPort",
    "ZOOKEEPER_HOSTS" -> s"$localhost:$zkPort",
    "TSS_PORT" -> ttsPort,
    "AGENTS_HOST" -> localhost,
    "AGENTS_PORTS" -> agentsPort,
    "HTTP_PORT" -> restPort,
    "RESTFUL_HOSTS" -> s"$localhost:$restPort",
    "ES_HOSTS" -> s"$localhost:$esPort",
    "JDBC_HOSTS" -> s"$localhost:$jdbcPort",
    "SILENT" -> "",
    "BENCHMARK_PORT" -> serverSocket.getLocalPort)
    .mapValues(_.toString)


  override def withFixture(test: NoArgTest): Outcome = {
    val zkServer = new TestingServer(zkPort, true)

    val ttsServer = new ClassRunner(classOf[Server], environment = commonEnvironment).start()
    Thread.sleep(waitingTimeout)

    val mongoServer = new EmbeddedMongo(mongoPort)
    mongoServer.start()

    val result = Try(super.withFixture(test))

    mongoServer.stop()
    ttsServer.destroy()
    zkServer.close()

    result.get
  }

  "Output-streaming module" should "send data to the RESTful server properly" in {
    val environment = commonEnvironment ++ Map(
      "INSTANCE_NAME" -> restInstanceName,
      "TASK_NAME" -> s"$restInstanceName-task0")

    def runClass(clazz: Class[_]): Process =
      new ClassRunner(clazz, environment = environment).start()

    val restServer = runClass(classOf[OutputTestRestServer])
    val waitResponseFromRunner = new Thread(() => serverSocket.accept())

    val result = Try {
      val setup = runClass(classOf[SjRestOutputModuleSetup])
      setup.waitFor() shouldBe 0

      waitResponseFromRunner.start()

      val runner = runClass(classOf[SjOutputModuleRunner])

      while (waitResponseFromRunner.isAlive && runner.isAlive)
        Thread.sleep(waitingTimeout)

      runner.isAlive shouldBe true

      Thread.sleep(waitingTimeout * 10)
      runner.destroy()

      val checker = runClass(classOf[RestDataChecker])
      checker.waitFor() shouldBe 0
    }

    if (waitResponseFromRunner.isAlive)
      waitResponseFromRunner.interrupt()
    restServer.destroy()

    result.get
  }

  it should "send data to the Elasticsearch properly" in {
    val environment = commonEnvironment ++ Map(
      "INSTANCE_NAME" -> esInstanceName,
      "TASK_NAME" -> s"$esInstanceName-task0")

    def runClass(clazz: Class[_]): Process =
      new ClassRunner(clazz, environment = environment).start()

    val waitResponseFromRunner = new Thread(() => serverSocket.accept())

    val esServer = new EmbeddedElasticsearch(esPort)
    esServer.start()

    val result = Try {
      val setup = runClass(classOf[SjESOutputModuleSetup])
      setup.waitFor() shouldBe 0

      waitResponseFromRunner.start()

      val runner = runClass(classOf[SjOutputModuleRunner])

      while (waitResponseFromRunner.isAlive && runner.isAlive)
        Thread.sleep(waitingTimeout)

      runner.isAlive shouldBe true

      Thread.sleep(waitingTimeout * 10)
      runner.destroy()

      val checker = runClass(classOf[ESDataChecker])
      checker.waitFor() shouldBe 0
    }

    esServer.stop()

    result.get
  }

  it should "send data to the SQL-database properly" in {
    val environment = commonEnvironment ++ Map(
      "INSTANCE_NAME" -> jdbcInstanceName,
      "TASK_NAME" -> s"$jdbcInstanceName-task0")

    def runClass(clazz: Class[_]): Process =
      new ClassRunner(clazz, environment = environment).start()

    val waitResponseFromRunner = new Thread(() => serverSocket.accept())

    val server = new EmbeddedPostgres(Version.V9_6_2)
    server.start("localhost", jdbcPort, databaseName, jdbcUsername, jdbcPassword)

    val result = Try {
      val setup = runClass(classOf[SjJDBCOutputModuleSetup])
      setup.waitFor() shouldBe 0

      waitResponseFromRunner.start()

      val runner = runClass(classOf[SjOutputModuleRunner])

      while (waitResponseFromRunner.isAlive && runner.isAlive)
        Thread.sleep(waitingTimeout)

      runner.isAlive shouldBe true

      Thread.sleep(waitingTimeout * 10)
      runner.destroy()

      val checker = runClass(classOf[JDBCDataChecker])
      checker.waitFor() shouldBe 0
    }

    server.stop()

    result.get
  }
}
