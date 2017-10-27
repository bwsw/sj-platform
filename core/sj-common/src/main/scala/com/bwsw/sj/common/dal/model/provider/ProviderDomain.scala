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

import java.net.{InetSocketAddress, Socket, URI}
import java.nio.channels.ClosedChannelException
import java.util.{Collections, Date}

import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.{IdField, PropertyField}
import com.bwsw.sj.common.utils.{MessageResourceUtils, ProviderLiterals}
import kafka.javaapi.TopicMetadataRequest
import kafka.javaapi.consumer.SimpleConsumer
import org.apache.zookeeper.ZooKeeper
import org.mongodb.morphia.annotations.Entity
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}


/**
  * protected methods and variables need for testing purposes
  */
@Entity("providers")
class ProviderDomain(@IdField val name: String,
                     val description: String,
                     val hosts: Array[String],
                     @PropertyField("provider-type") val providerType: String,
                     val creationDate: Date) {

  import ProviderDomain._

  def getConcatenatedHosts(separator: String = ","): String = {
    hosts.mkString(separator)
  }

  def checkConnection(zkSessionTimeout: Int)(implicit injector: Injector): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    for (host <- this.hosts) {
      errors ++= checkProviderConnectionByType(host, this.providerType, zkSessionTimeout)
    }

    errors
  }

  protected def checkProviderConnectionByType(host: String, providerType: String, zkSessionTimeout: Int)
                                             (implicit injector: Injector): ArrayBuffer[String] = {
    providerType match {
      case ProviderLiterals.zookeeperType =>
        checkZookeeperConnection(host, zkSessionTimeout)
      case ProviderLiterals.kafkaType =>
        checkKafkaConnection(host)
      case ProviderLiterals.elasticsearchType =>
        checkESConnection(host)
      case ProviderLiterals.jdbcType =>
        checkJdbcConnection(host)
      case ProviderLiterals.restType =>
        checkRestConnection(host)
      case _ =>
        throw new Exception(s"Host checking for provider type '$providerType' is not implemented")
    }
  }

  protected def checkZookeeperConnection(address: String, zkSessionTimeout: Int): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()

    Try(new ZooKeeper(address, zkSessionTimeout, null)) match {
      case Success(client) =>
        val deadline = 1.seconds.fromNow
        var connected: Boolean = false
        while (!connected && deadline.hasTimeLeft) {
          connected = client.getState.isConnected
        }
        if (!connected) {
          errors += messageResourceUtils.createMessage("rest.providers.provider.cannot.connect.zk", address)
        }
        client.close()
      case Failure(_) =>
        errors += messageResourceUtils.createMessage("rest.providers.provider.cannot.connect.zk.wrong.host", address)
    }

    errors
  }

  protected def checkKafkaConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    val (host, port) = getHostAndPort(address)
    val consumer = new SimpleConsumer(host, port, 500, 64 * 1024, "connectionTest")
    val topics = Collections.singletonList("test_connection")
    val req = new TopicMetadataRequest(topics)
    Try(consumer.send(req)) match {
      case Success(_) =>
      case Failure(_: ClosedChannelException) | Failure(_: java.io.EOFException) =>
        errors += messageResourceUtils.createMessage("rest.providers.provider.cannot.connect.kafka", address)
      case Failure(_) =>
        errors += messageResourceUtils.createMessage("rest.providers.provider.cannot.connect.kafka.wrong.host", address)
    }

    errors
  }

  protected def checkESConnection(address: String): ArrayBuffer[String] = ArrayBuffer()

  protected def checkJdbcConnection(address: String)(implicit injector: Injector): ArrayBuffer[String] = ArrayBuffer()

  protected def checkRestConnection(address: String): ArrayBuffer[String] = {
    val (host, port) = getHostAndPort(address)
    val socket = new Socket()
    val errors = Try {
      socket.connect(new InetSocketAddress(host, port), ProviderLiterals.connectTimeoutMillis)
    } match {
      case Success(_) =>
        ArrayBuffer[String]()
      case Failure(_) =>
        ArrayBuffer[String](messageResourceUtils.createMessage("rest.providers.provider.cannot.connect.rest", address))
    }
    if (!socket.isClosed) socket.close()

    errors
  }

  protected def getHostAndPort(address: String): (String, Int) = {
    val uri = new URI("dummy://" + address)
    val host = uri.getHost
    val port = uri.getPort

    (host, port)
  }
}

object ProviderDomain {
  protected[provider] val messageResourceUtils = new MessageResourceUtils
}