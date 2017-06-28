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
package com.bwsw.common

import java.util.Properties

import com.bwsw.sj.common.config.ConfigLiterals
import kafka.admin.AdminUtils
import kafka.utils.ZkUtils
import org.I0Itec.zkclient.ZkConnection
import org.apache.kafka.common.requests.MetadataResponse.TopicMetadata

/**
  * Provides methods to CRUD kafka topics using [[AdminUtils]] and [[ZkUtils]]
  *
  * @param zkServers set of zookeeper servers
  * @param timeout   zookeeper session or connection timeout
  */
class KafkaClient(zkServers: Array[String], timeout: Int = ConfigLiterals.zkSessionTimeoutDefault) {
  private val splitZkServers = zkServers.mkString(";")
  private val zkConnect = new ZkConnection(splitZkServers)
  private val zkClient = ZkUtils.createZkClient(splitZkServers, timeout, timeout)
  private val zkUtils = new ZkUtils(zkClient, zkConnect, false)

  def topicExists(topic: String): Boolean = {
    AdminUtils.topicExists(zkUtils, topic)
  }

  def createTopic(topic: String, partitions: Int, replicationFactor: Int): Unit = {
    AdminUtils.createTopic(zkUtils, topic, partitions, replicationFactor, new Properties())
  }

  def fetchTopicMetadataFromZk(topic: String): TopicMetadata = {
    AdminUtils.fetchTopicMetadataFromZk(topic, zkUtils)
  }

  def deleteTopic(topic: String): Unit = {
    AdminUtils.deleteTopic(zkUtils, topic)
  }

  def close(): Unit = {
    zkUtils.close()
  }
}
