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
