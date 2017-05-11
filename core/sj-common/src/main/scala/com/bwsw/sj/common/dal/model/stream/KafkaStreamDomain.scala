package com.bwsw.sj.common.dal.model.stream

import java.util.Properties

import com.bwsw.sj.common.dal.model.service.KafkaServiceDomain
import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.bwsw.sj.common.rest.model.stream.KafkaStreamApi
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import kafka.admin.AdminUtils
import kafka.common.TopicAlreadyMarkedForDeletionException
import kafka.utils.ZkUtils
import org.I0Itec.zkclient.ZkConnection

class KafkaStreamDomain(override val name: String,
                        override val service: KafkaServiceDomain,
                        val partitions: Int,
                        val replicationFactor: Int,
                        override val description: String = RestLiterals.defaultDescription,
                        override val force: Boolean = false,
                        override val tags: Array[String] = Array())
  extends StreamDomain(name, description, service, force, tags, StreamLiterals.kafkaStreamType) {

  override def asProtocolStream(): KafkaStreamApi =
    new KafkaStreamApi(
      name = name,
      service = service.name,
      tags = tags,
      force = force,
      description = description,
      partitions = partitions,
      replicationFactor = replicationFactor
    )

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
