package com.bwsw.sj.common.dal.model.stream

import java.util.Properties

import com.bwsw.sj.common.dal.model.service.KafkaServiceDomain
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import kafka.admin.AdminUtils
import kafka.common.TopicAlreadyMarkedForDeletionException
import kafka.utils.ZkUtils
import org.I0Itec.zkclient.ZkConnection

import scala.util.{Failure, Success, Try}

class KafkaStreamDomain(override val name: String,
                        override val service: KafkaServiceDomain,
                        val partitions: Int,
                        val replicationFactor: Int,
                        override val description: String = RestLiterals.defaultDescription,
                        override val force: Boolean = false,
                        override val tags: Array[String] = Array(),
                        private val zkSessionTimeout: Int = 7000)
  extends StreamDomain(name, description, service, force, tags, StreamLiterals.kafkaStreamType) {

  override def create(): Unit = {
    Try {
      val zkUtils = createZkUtils()
      if (!AdminUtils.topicExists(zkUtils, this.name)) {
        AdminUtils.createTopic(zkUtils, this.name, this.partitions, this.replicationFactor, new Properties())
      }
    } match {
      case Success(_) =>
      case Failure(_: TopicAlreadyMarkedForDeletionException) =>
        throw new Exception(s"Cannot create a kafka topic ${this.name}. Topic is marked for deletion. It means that kafka doesn't support deletion")
      case Failure(e) => throw e
    }
  }

  private def createZkUtils(): ZkUtils = {
    val zkHost = this.service.zkProvider.hosts
    val zkConnect = new ZkConnection(zkHost.mkString(";"))
    val zkClient = ZkUtils.createZkClient(zkHost.mkString(";"), zkSessionTimeout, zkSessionTimeout)
    val zkUtils = new ZkUtils(zkClient, zkConnect, false)

    zkUtils
  }
}
