package com.bwsw.sj.common.dal.model.stream

import java.util.Properties

import com.bwsw.sj.common.dal.model.service.KafkaService
import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.bwsw.sj.common.rest.model.stream.KafkaStreamData
import com.bwsw.sj.common.utils.StreamLiterals
import kafka.admin.AdminUtils
import kafka.common.TopicAlreadyMarkedForDeletionException
import kafka.utils.ZkUtils
import org.I0Itec.zkclient.ZkConnection

class KafkaSjStream(override val name: String,
                    override val service: KafkaService,
                    val partitions: Int,
                    val replicationFactor: Int,
                    override val description: String = "No description",
                    override val force: Boolean = false,
                    override val tags: Array[String] = Array(),
                    override val streamType: String = StreamLiterals.kafkaStreamType)
  extends SjStream(name, description, service, force, tags, streamType) {

  override def asProtocolStream(): KafkaStreamData = {
    val streamData = new KafkaStreamData
    super.fillProtocolStream(streamData)

    streamData.partitions = this.partitions
    streamData.replicationFactor = this.replicationFactor

    streamData
  }

  override def create(): Unit = {
    try {
      val zkUtils = createZkUtils()
      if (!AdminUtils.topicExists(zkUtils, this.name)) {
        AdminUtils.createTopic(zkUtils, this.name, this.partitions, this.replicationFactor, new Properties())
      }
    } catch {
      case ex: TopicAlreadyMarkedForDeletionException =>
        throw new Exception(s"Cannot create a kafka topic ${this.name}. Topic is marked for deletion. It means that kafka doesn't support deletion")
    }
  }

  override def delete(): Unit = {
    try {
      val zkUtils = createZkUtils()
      if (AdminUtils.topicExists(zkUtils, this.name)) {
        AdminUtils.deleteTopic(zkUtils, this.name)
      }
    } catch {
      case ex: TopicAlreadyMarkedForDeletionException =>
        throw new Exception(s"Cannot delete a kafka topic '${this.name}'. Topic is already marked for deletion. It means that kafka doesn't support deletion")
    }
  }

  private def createZkUtils(): ZkUtils = {
    val zkHost = this.service.zkProvider.hosts
    val zkConnect = new ZkConnection(zkHost.mkString(";"))
    val zkTimeout = ConfigurationSettingsUtils.getZkSessionTimeout()
    val zkClient = ZkUtils.createZkClient(zkHost.mkString(";"), zkTimeout, zkTimeout)
    val zkUtils = new ZkUtils(zkClient, zkConnect, false)

    zkUtils
  }
}
