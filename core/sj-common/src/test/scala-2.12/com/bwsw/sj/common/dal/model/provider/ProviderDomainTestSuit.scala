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

import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.utils.ProviderLiterals
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}

import scala.collection.mutable.ArrayBuffer

class ProviderDomainTestSuit extends FlatSpec with Matchers with PrivateMethodTester with ProviderDomainMocks {
  it should "getConcatenatedHosts() method returns provider hosts linked with defined separator" in {
    //arrange
    val separator = "=="
    val expectedHosts = Array("host1", "host2")
    val provider = providerDomain(setOfHosts = expectedHosts)

    //act
    val hosts = provider.getConcatenatedHosts(separator)

    //assert
    hosts shouldBe expectedHosts.mkString(separator)
  }

  it should "getHostAndPort() method extract a host and a port from the passed address" in {
    //arrange
    val getHostAndPort = PrivateMethod[(String, Int)]('getHostAndPort)
    val expectedHost = "host"
    val expectedPort = 8080
    val provider = providerDomain()

    //act
    val (host, port) = provider invokePrivate getHostAndPort(expectedHost + ":" + expectedPort)

    //assert
    host shouldBe expectedHost
    port shouldBe expectedPort
  }

  it should s"checkConnection() method checks jdbc connection for each host " +
    s"if provider has got '${ProviderLiterals.jdbcType}' type" in {
    //arrange
    val numberOfChecks = 4
    val provider = mock[ProviderDomainMock]
    when(provider.providerType).thenReturn(ProviderLiterals.jdbcType)
    when(provider.hosts).thenReturn(Array.fill(numberOfChecks)("example"))
    when(provider.checkConnection(any())).thenCallRealMethod()
    when(provider.checkProviderConnectionByType(any(), any(), any())).thenCallRealMethod()
    when(provider.checkJdbcConnection(any())).thenCallRealMethod()

    //act
    provider.checkConnection(ConfigLiterals.zkSessionTimeoutDefault)

    //assert
    verify(provider, times(numberOfChecks)).checkJdbcConnection(any())
  }


  it should s"checkConnection() method checks elasticsearch connection for each host " +
    s"if provider has got '${ProviderLiterals.elasticsearchType}' type" in {
    //arrange
    val numberOfChecks = 4
    val provider = mock[ProviderDomainMock]
    when(provider.providerType).thenReturn(ProviderLiterals.elasticsearchType)
    when(provider.hosts).thenReturn(Array.fill(numberOfChecks)("example"))
    when(provider.checkConnection(any())).thenCallRealMethod()
    when(provider.checkProviderConnectionByType(any(), any(), any())).thenCallRealMethod()
    when(provider.checkESConnection(any())).thenCallRealMethod()

    //act
    provider.checkConnection(ConfigLiterals.zkSessionTimeoutDefault)

    //assert
    verify(provider, times(numberOfChecks)).checkESConnection(any())
  }

  it should s"checkConnection() method checks kafka connection for each host " +
    s"if provider has got '${ProviderLiterals.kafkaType}' type" in {
    //arrange
    val numberOfChecks = 4
    val provider = mock[ProviderDomainMock]
    when(provider.providerType).thenReturn(ProviderLiterals.kafkaType)
    when(provider.hosts).thenReturn(Array.fill(numberOfChecks)("example"))
    when(provider.checkConnection(any())).thenCallRealMethod()
    when(provider.checkProviderConnectionByType(any(), any(), any())).thenCallRealMethod()
    when(provider.checkKafkaConnection(any())).thenCallRealMethod()

    //act
    provider.checkConnection(ConfigLiterals.zkSessionTimeoutDefault)

    //assert
    verify(provider, times(numberOfChecks)).checkKafkaConnection(any())
  }

  it should s"checkConnection() method checks http connection for each host " +
    s"if provider has got '${ProviderLiterals.restType}' type" in {
    //arrange
    val numberOfChecks = 4
    val provider = mock[ProviderDomainMock]
    when(provider.providerType).thenReturn(ProviderLiterals.restType)
    when(provider.hosts).thenReturn(Array.fill(numberOfChecks)("example"))
    when(provider.checkConnection(any())).thenCallRealMethod()
    when(provider.checkProviderConnectionByType(any(), any(), any())).thenCallRealMethod()
    when(provider.checkRestConnection(any())).thenCallRealMethod()

    //act
    provider.checkConnection(ConfigLiterals.zkSessionTimeoutDefault)

    //assert
    verify(provider, times(numberOfChecks)).checkRestConnection(any())
  }

  it should s"checkConnection() method checks zookeeper connection for each host " +
    s"if provider has got '${ProviderLiterals.zookeeperType}' type" in {
    //arrange
    val numberOfChecks = 4
    val provider = mock[ProviderDomainMock]
    when(provider.providerType).thenReturn(ProviderLiterals.zookeeperType)
    when(provider.hosts).thenReturn(Array.fill(numberOfChecks)("example"))
    when(provider.checkConnection(any())).thenCallRealMethod()
    when(provider.checkProviderConnectionByType(any(), any(), any())).thenCallRealMethod()
    when(provider.checkZookeeperConnection(any(), any())).thenCallRealMethod()

    //act
    provider.checkConnection(ConfigLiterals.zkSessionTimeoutDefault)

    //assert
    verify(provider, times(numberOfChecks)).checkZookeeperConnection(any(), any())
  }
}

trait ProviderDomainMocks extends MockitoSugar {
  def providerDomain(setOfHosts: Array[String] = Array()) =
    new ProviderDomain(null, null, setOfHosts, null, null)
}

class ProviderDomainMock extends ProviderDomain(null, null, Array("host"), null, null) {
  override def checkESConnection(address: String): ArrayBuffer[String] = ArrayBuffer()

  override def checkRestConnection(address: String): ArrayBuffer[String] = ArrayBuffer()

  override def checkJdbcConnection(address: String): ArrayBuffer[String] = ArrayBuffer()

  override def checkKafkaConnection(address: String): ArrayBuffer[String] = ArrayBuffer()

  override def checkZookeeperConnection(address: String, zkSessionTimeout: Int): ArrayBuffer[String] = ArrayBuffer()

  override def checkProviderConnectionByType(host: String, providerType: String, zkSessionTimeout: Int): ArrayBuffer[String] =
    super.checkProviderConnectionByType(host, providerType, zkSessionTimeout)
}