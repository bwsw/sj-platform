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

import com.bwsw.common.embedded.{EmbeddedKafka, EmbeddedMongo}
import com.bwsw.sj.common.MongoAuthChecker
import com.bwsw.sj.common.config.{ConfigLiterals, SettingsUtils}
import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.FileMetadataLiterals
import com.bwsw.sj.common.utils.ProviderLiterals
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

  val zkTimeout = 1000
  val zkServer1 = new TestingServer(false)
  val zkServer2 = new TestingServer(false)


  def withZkServer(testCode: (ProviderDomain, ServerWrapper, ServerWrapper) => Assertion): Assertion = {
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

    val result = Try(testCode(provider, wrappedServer1, wrappedServer2))

    wrappedServer1.stop()
    wrappedServer2.stop()

    result.get
  }

  "ProviderDomain" should "check connection to the ZooKeeper server properly" in withZkServer(testProviderConnection)


  def withRestServer(testCode: (ProviderDomain, ServerWrapper, ServerWrapper) => Assertion): Assertion = {
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

    val result = Try(testCode(provider, wrappedServer1, wrappedServer2))

    wrappedServer1.stop()
    wrappedServer2.stop()

    result.get
  }

  it should "check connection to the RESTful server properly" in withRestServer(testProviderConnection)


  def withKafkaServer(testCode: (ProviderDomain, ServerWrapper, ServerWrapper) => Assertion): Assertion = {
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

    val result = Try(testCode(provider, wrappedServer1, wrappedServer2))

    wrappedServer1.stop()
    wrappedServer2.stop()
    zkServer1.stop()
    zkServer2.stop()

    result.get
  }

  it should "check connection to the Kafka server properly" in withKafkaServer(testProviderConnection)


  def withSqlDatabase(testCode: (JDBCProviderDomain, ServerWrapper, ServerWrapper) => Assertion): Assertion = {
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

    val result = Try(testCode(provider, wrappedServer1, wrappedServer2))

    wrappedServer1.stop()
    wrappedServer2.stop()
    mongoServer.stop()

    result.get
  }

  it should "check connection to the SQL database properly" in withSqlDatabase(testProviderConnection)


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
    s"Can gain an access to Zookeeper on '$address'"

  private def createRestConnectionError(address: String): String =
    s"Can not establish connection to Rest on '$address'"

  private def createKafkaConnectionError(address: String): String =
    s"Can not establish connection to Kafka on '$address'"

  private def createJdbcConnectionError(address: String): String =
    s"Cannot gain an access to JDBC on '$address'"

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
  private var serverRunned = true

  def stop(): Unit = {
    if (serverRunned) {
      stoppingMethod()
      serverRunned = false
    }
  }
}
