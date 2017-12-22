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
package com.bwsw.common.embedded

import java.io.File
import java.net.ServerSocket
import java.util.{Properties, UUID}

import kafka.server.{KafkaConfig, KafkaServer}
import org.apache.commons.io.FileUtils
import org.apache.curator.test.TestingServer

/**
  * Used for testing purposes only. Local Kafka server.
  *
  * @param zooKeeperAddress address of ZooKeeper server. If it not set, will be created local ZooKeeper server.
  * @author Pavel Tomskikh
  */
class EmbeddedKafka(zooKeeperAddress: Option[String] = None) {

  private val zkServer: Option[TestingServer] = {
    if (zooKeeperAddress.isDefined) None
    else Some(new TestingServer(false))
  }

  val zkConnectionString: String = zooKeeperAddress match {
    case Some(address) => address
    case None => zkServer.get.getConnectString
  }

  private val randomSocket = new ServerSocket(0)

  val port: Int = randomSocket.getLocalPort
  randomSocket.close()

  private val logDir = new File("logs/kafka/" + UUID.randomUUID().toString)

  private val kafkaServerProperties = new Properties
  kafkaServerProperties.put("port", Int.box(port))
  kafkaServerProperties.put("zookeeper.connect", zkConnectionString)
  kafkaServerProperties.put("log.dir", logDir.getAbsolutePath)

  private val kafkaServer = new KafkaServer(KafkaConfig.fromProps(kafkaServerProperties))

  def start(): Unit = {
    zkServer.foreach(_.start())
    kafkaServer.startup()
  }

  def stop(): Unit = {
    kafkaServer.shutdown()
    kafkaServer.awaitShutdown()
    zkServer.foreach(_.close())

    FileUtils.deleteDirectory(logDir)
  }
}
