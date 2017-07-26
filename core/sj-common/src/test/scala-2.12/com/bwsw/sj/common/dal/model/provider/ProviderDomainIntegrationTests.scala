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

import java.util.Date

import com.bwsw.common.embedded.EmbeddedKafka
import com.bwsw.sj.common.utils.ProviderLiterals
import org.apache.curator.test.TestingServer
import org.mockserver.integration.ClientAndServer
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.collection.mutable.ArrayBuffer

/**
  * Integration tests for [[ProviderDomain]]
  *
  * @author Pavel Tomskikh
  */
class ProviderDomainIntegrationTests extends FlatSpec with Matchers with BeforeAndAfterAll {

  val zkTimeout = 1000
  val zkServer1 = new TestingServer(false)
  val zkServer2 = new TestingServer(false)

  "ProviderDomain" should "check connection to the ZooKeeper server properly" in {
    zkServer1.restart()
    zkServer2.restart()

    val provider = new ProviderDomain(
      "zk-provider",
      "description",
      Array(zkServer1.getConnectString, zkServer2.getConnectString),
      null,
      null,
      ProviderLiterals.zookeeperType,
      new Date())

    val error1 = createZkConnectionError(zkServer1.getConnectString)
    val error2 = createZkConnectionError(zkServer2.getConnectString)

    provider.checkConnection(zkTimeout) shouldBe empty

    zkServer1.stop()

    provider.checkConnection(zkTimeout) shouldBe ArrayBuffer(error1)

    zkServer2.stop()

    provider.checkConnection(zkTimeout) shouldBe ArrayBuffer(error1, error2)
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

    val error1 = createRestConnectionError(address1)
    val error2 = createRestConnectionError(address2)

    provider.checkConnection(zkTimeout) shouldBe empty

    server1.stop()

    provider.checkConnection(zkTimeout) shouldBe ArrayBuffer(error1)

    server2.stop()

    provider.checkConnection(zkTimeout) shouldBe ArrayBuffer(error1, error2)
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

    val error1 = createKafkaConnectionError(address1)
    val error2 = createKafkaConnectionError(address2)

    provider.checkConnection(zkTimeout) shouldBe empty

    server1.stop()

    provider.checkConnection(zkTimeout) shouldBe ArrayBuffer(error1)

    server2.stop()

    provider.checkConnection(zkTimeout) shouldBe ArrayBuffer(error1, error2)
  }


  override def afterAll(): Unit = {
    zkServer1.close()
    zkServer2.close()
  }


  private def createZkConnectionError(address: String): String =
    s"Can gain an access to Zookeeper on '$address'"

  private def createRestConnectionError(address: String): String =
    s"Can not establish connection to Rest on '$address'"

  private def createKafkaConnectionError(address: String): String =
    s"Can not establish connection to Kafka on '$address'"
}
