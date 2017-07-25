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

import com.bwsw.sj.common.utils.ProviderLiterals
import org.apache.curator.test.TestingServer
import org.mockserver.integration.ClientAndServer
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.ArrayBuffer

/**
  * Integration tests for [[ProviderDomain]]
  *
  * @author Pavel Tomskikh
  */
class ProviderDomainIntegrationTests extends FlatSpec with Matchers {

  val zkTimeout = 1000

  "ProviderDomain" should "check connection to zookeeper server properly" in {
    val server1 = new TestingServer(true)
    val server2 = new TestingServer(true)

    val provider = new ProviderDomain(
      "zk-provider",
      "description",
      Array(server1.getConnectString, server2.getConnectString),
      null,
      null,
      ProviderLiterals.zookeeperType,
      new Date())

    val error1 = createZkConnectionError(server1.getConnectString)
    val error2 = createZkConnectionError(server2.getConnectString)

    provider.checkConnection(zkTimeout) shouldBe empty

    server1.close()

    provider.checkConnection(zkTimeout) shouldBe ArrayBuffer(error1)

    server2.close()

    provider.checkConnection(zkTimeout) shouldBe ArrayBuffer(error1, error2)
  }

  it should "check connection to RESTful server properly" in {
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


  private def createZkConnectionError(address: String): String =
    s"Can gain an access to Zookeeper on '$address'"

  private def createRestConnectionError(address: String): String =
    s"Can not establish connection to Rest on '$address'"
}
