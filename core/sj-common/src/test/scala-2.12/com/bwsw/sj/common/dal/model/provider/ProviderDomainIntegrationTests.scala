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
package com.bwsw.sj.common.dal.model.provider

import java.io.File
import java.net.URL
import java.util.{Date, UUID}

import com.bwsw.common.embedded.{EmbeddedElasticsearch, EmbeddedKafka, EmbeddedMongo}
import com.bwsw.sj.common.MongoAuthChecker
import com.bwsw.sj.common.config.{ConfigLiterals, SettingsUtils}
import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.FileMetadataLiterals
import com.bwsw.sj.common.utils.{MessageResourceUtils, ProviderLiterals}
import org.apache.commons.io.FileUtils
import org.apache.curator.test.TestingServer
import org.mockserver.integration.ClientAndServer
import org.scalatest.{Assertion, BeforeAndAfterAll, FlatSpec, Matchers}
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres
import ru.yandex.qatools.embed.postgresql.distribution.Version
import ru.yandex.qatools.embed.postgresql.util.SocketUtil
import scaldi.Module

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

/**
  * Integration tests for [[ProviderDomain]]
  *
  * @author Pavel Tomskikh
  */
class ProviderDomainIntegrationTests extends FlatSpec with Matchers with BeforeAndAfterAll {

  val messageResourceUtils = new MessageResourceUtils

  val zkTimeout = 1000
  val zkServer1 = new TestingServer(false)
  val zkServer2 = new TestingServer(false)


  "ProviderDomain" should "check connection to the ZooKeeper server properly" in {
    zkServer1.restart()
    zkServer2.restart()

    val address1 = zkServer1.getConnectString
    val address2 = zkServer2.getConnectString

    val provider = new ProviderDomain(
      "zk-provider",
      "description",
      Array(address1, address2),
      null,
      null,
      ProviderLiterals.zookeeperType,
      new Date())

    val wrappedServer1 = new ServerWrapper(createZkConnectionError(address1), () => zkServer1.stop())
    val wrappedServer2 = new ServerWrapper(createZkConnectionError(address2), () => zkServer2.stop())

    val result = Try(testProviderConnection(provider, wrappedServer1, wrappedServer2))

    wrappedServer1.stop()
    wrappedServer2.stop()

    result.get
  }


  it should "check connection to the RESTful server properly" in {
    val server1 = new ClientAndServer()
    val server2 = new ClientAndServer()
    val address1 = "localhost:" + server1.getPort
    val address2 = "localhost:" + server2.getPort

    val provider = new ProviderDomain(
      "rest-provider",
      "description",
      Array(address1, address2),
      null,
      null,
      ProviderLiterals.restType,
      new Date())

    val wrappedServer1 = new ServerWrapper(createRestConnectionError(address1), () => server1.stop())
    val wrappedServer2 = new ServerWrapper(createRestConnectionError(address2), () => server2.stop())

    val result = Try(testProviderConnection(provider, wrappedServer1, wrappedServer2))

    wrappedServer1.stop()
    wrappedServer2.stop()

    result.get
  }

  it should "check connection to the Kafka server properly" in {
    zkServer1.restart()
    zkServer2.restart()

    val server1 = new EmbeddedKafka(Some(zkServer1.getConnectString))
    val server2 = new EmbeddedKafka(Some(zkServer2.getConnectString))
    val address1 = "localhost:" + server1.port
    val address2 = "localhost:" + server2.port

    server1.start()
    server2.start()

    val provider = new ProviderDomain(
      "kafka-provider",
      "description",
      Array(address1, address2),
      null,
      null,
      ProviderLiterals.kafkaType,
      new Date())

    val wrappedServer1 = new ServerWrapper(createKafkaConnectionError(address1), () => server1.stop())
    val wrappedServer2 = new ServerWrapper(createKafkaConnectionError(address2), () => server2.stop())

    val result = Try(testProviderConnection(provider, wrappedServer1, wrappedServer2))

    wrappedServer1.stop()
    wrappedServer2.stop()
    zkServer1.stop()
    zkServer2.stop()

    result.get
  }

  it should "check connection to the PostgreSQL database properly" in {
    val mongoPort = SocketUtil.findFreePort()
    val mongoServer = new EmbeddedMongo(mongoPort)
    mongoServer.start()

    val connectionRepository = createConnectionRepository(mongoPort)

    val injector = new Module {
      bind[ConnectionRepository] to connectionRepository
      bind[SettingsUtils] to new SettingsUtils()
    }.injector

    val user = "test-user"
    val password = "test-password"
    val database = "testDB"
    val version = Version.V9_6_2

    val server1 = new EmbeddedPostgres(version)
    val server1Port = SocketUtil.findFreePort()
    server1.start("localhost", server1Port, database, user, password)

    val server2 = new EmbeddedPostgres(version)
    val server2Port = SocketUtil.findFreePort()
    server2.start("localhost", server2Port, database, user, password)

    val address1 = "localhost:" + server1Port
    val address2 = "localhost:" + server2Port

    val driverName = "postgresql"
    loadJdbcDriver(connectionRepository, driverName)

    val provider = new JDBCProviderDomain(
      "rest-provider",
      "description",
      Array(address1, address2),
      user,
      password,
      driverName,
      new Date())(injector)

    val wrappedServer1 = new ServerWrapper(createJdbcConnectionError(address1), () => server1.stop())
    val wrappedServer2 = new ServerWrapper(createJdbcConnectionError(address2), () => server2.stop())

    val result = Try(testProviderConnection(provider, wrappedServer1, wrappedServer2))

    wrappedServer1.stop()
    wrappedServer2.stop()
    mongoServer.stop()

    result.get
  }

  it should "check connection to the Elasticsearch database properly" in {
    val server1 = new EmbeddedElasticsearch(SocketUtil.findFreePort())
    server1.start()
    val server2 = new EmbeddedElasticsearch(SocketUtil.findFreePort())
    server2.start()

    val address1 = "localhost:" + server1.port
    val address2 = "localhost:" + server2.port

    val provider = new ProviderDomain(
      "es-provider",
      "description",
      Array(address1, address2),
      null,
      null,
      ProviderLiterals.elasticsearchType,
      new Date())

    val wrappedServer1 = new ServerWrapper(createEsConnectionError(address1), () => server1.stop())
    val wrappedServer2 = new ServerWrapper(createEsConnectionError(address2), () => server2.stop())

    val result = Try(testProviderConnection(provider, wrappedServer1, wrappedServer2))

    wrappedServer1.stop()
    wrappedServer2.stop()

    result.get
  }


  override def afterAll(): Unit = {
    zkServer1.close()
    zkServer2.close()
  }


  private def testProviderConnection(provider: ProviderDomain, server1: ServerWrapper, server2: ServerWrapper): Assertion = {
    provider.checkConnection(zkTimeout) shouldBe empty

    server1.stop()

    provider.checkConnection(zkTimeout) shouldBe ArrayBuffer(server1.connectionError)

    server2.stop()

    provider.checkConnection(zkTimeout) shouldBe ArrayBuffer(server1.connectionError, server2.connectionError)
  }

  private def createZkConnectionError(address: String): String =
    messageResourceUtils.createMessage("rest.providers.provider.cannot.connect.zk", address)

  private def createRestConnectionError(address: String): String =
    messageResourceUtils.createMessage("rest.providers.provider.cannot.connect.rest", address)

  private def createKafkaConnectionError(address: String): String =
    messageResourceUtils.createMessage("rest.providers.provider.cannot.connect.kafka", address)

  private def createJdbcConnectionError(address: String): String =
    messageResourceUtils.createMessage("rest.providers.provider.cannot.connect.jdbc", address)

  private def createEsConnectionError(address: String): String =
    messageResourceUtils.createMessage("rest.providers.provider.cannot.connect.es", address)


  private def createConnectionRepository(mongoPort: Int): ConnectionRepository = {
    val mongoAddress = "localhost:" + mongoPort
    val mongoDatabase = "stream-juggler-test"
    val mongoAuthChecker = new MongoAuthChecker(mongoAddress, mongoDatabase)

    new ConnectionRepository(mongoAuthChecker, mongoAddress, mongoDatabase, None, None)
  }

  private def loadJdbcDriver(connectionRepository: ConnectionRepository, driverName: String): Unit = {
    val driverUrl = new URL("https://jdbc.postgresql.org/download/postgresql-42.1.3.jar")
    val driverFilename = s"postgresql-driver-${UUID.randomUUID().toString}.jar"
    val driverFile = new File(driverFilename)
    FileUtils.copyURLToFile(driverUrl, driverFile)

    connectionRepository.getFileStorage.put(
      driverFile,
      driverFilename,
      Map.empty[String, Any],
      FileMetadataLiterals.customFileType)

    driverFile.delete()

    val driverFilenameConfig = ConfigLiterals.getDriverFilename(driverName)
    val driverClassConfig = ConfigLiterals.getDriverClass(driverName)
    val driverPrefixConfig = ConfigLiterals.getDriverPrefix(driverName)

    val configService = connectionRepository.getConfigRepository
    configService.save(ConfigurationSettingDomain(driverFilenameConfig, driverFilename, ConfigLiterals.jdbcDomain, new Date()))
    configService.save(ConfigurationSettingDomain(driverClassConfig, "org.postgresql.Driver", ConfigLiterals.jdbcDomain, new Date()))
    configService.save(ConfigurationSettingDomain(driverPrefixConfig, "jdbc:postgresql", ConfigLiterals.jdbcDomain, new Date()))
  }
}

class ServerWrapper(val connectionError: String, stoppingMethod: () => Unit) {
  private var serverRun = true

  def stop(): Unit = {
    if (serverRun) {
      stoppingMethod()
      serverRun = false
    }
  }
}
