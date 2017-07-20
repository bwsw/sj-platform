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

import java.net.{Socket, URI}
import java.nio.channels.ClosedChannelException
import java.util.{Collections, Date}

import com.bwsw.common.es.ElasticsearchClient
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.{IdField, PropertyField}
import com.bwsw.sj.common.utils.ProviderLiterals
import kafka.javaapi.TopicMetadataRequest
import kafka.javaapi.consumer.SimpleConsumer
import org.apache.zookeeper.ZooKeeper
import org.mongodb.morphia.annotations.Entity

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
                     val login: String,
                     val password: String,
                     @PropertyField("provider-type") val providerType: String,
                     val creationDate: Date) {

  def getConcatenatedHosts(separator: String = ","): String = {
    hosts.mkString(separator)
  }

  def checkConnection(zkSessionTimeout: Int): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    for (host <- this.hosts) {
      errors ++= checkProviderConnectionByType(host, this.providerType, zkSessionTimeout)
    }

    errors
  }

  protected def checkProviderConnectionByType(host: String, providerType: String, zkSessionTimeout: Int): ArrayBuffer[String] = {
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
          errors += s"Can gain an access to Zookeeper on '$address'"
        }
        client.close()
      case Failure(_) =>
        errors += s"Wrong host '$address'"
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
        errors += s"Can not establish connection to Kafka on '$address'"
      case Failure(_) =>
        errors += s"Some issues encountered while trying to establish connection to '$address'"
    }

    errors
  }

  protected def checkESConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    val client = new ElasticsearchClient(Set(getHostAndPort(address)))
    if (!client.isConnected()) {
      errors += s"Can not establish connection to ElasticSearch on '$address'"
    }
    client.close()

    errors
  }

  protected def checkJdbcConnection(address: String): ArrayBuffer[String] = ArrayBuffer()

  protected def checkRestConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    val (host, port) = getHostAndPort(address)
    var socket: Option[Socket] = None
    Try {
       socket = Some(new Socket(host, port))
    } match {
      case Success(_) =>
      case Failure(a) =>
        errors += s"Can not establish connection to Rest on '$address'"
    }
    socket.foreach(_.close())

    errors
  }

  private def getHostAndPort(address: String): (String, Int) = {
    val uri = new URI("dummy://" + address)
    val host = uri.getHost()
    val port = uri.getPort()

    (host, port)
  }
}
