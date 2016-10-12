package com.bwsw.sj.common.rest.entities.stream

import java.util.Properties

import com.bwsw.sj.common.DAL.model.{KafkaService, KafkaSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{ConfigSettingsUtils, ServiceLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import kafka.admin.AdminUtils
import kafka.common.{TopicAlreadyMarkedForDeletionException, TopicExistsException}
import kafka.utils.ZkUtils
import org.I0Itec.zkclient.ZkConnection

import scala.collection.mutable.ArrayBuffer

class KafkaSjStreamData() extends SjStreamData() {
  streamType = StreamLiterals.kafkaStreamType
  var partitions: Int = Int.MinValue
  @JsonProperty("replication-factor") var replicationFactor: Int = Int.MinValue

  override def validate() = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    Option(this.service) match {
      case None =>
        errors += s"'Service' is required"
      case Some(x) =>
        val serviceObj = serviceDAO.get(this.service)
        serviceObj match {
          case None =>
            errors += s"Service '${this.service}' does not exist"
          case Some(modelService) =>
            if (modelService.serviceType != ServiceLiterals.kafkaType) {
              errors += s"Service for ${StreamLiterals.kafkaStreamType} stream " +
                s"must be of '${ServiceLiterals.kafkaType}' type ('${modelService.serviceType}' is given instead)"
            } else {
              errors ++= checkStreamPartitionsOnConsistency(modelService.asInstanceOf[KafkaService])
            }
        }
    }

    //partitions
    if (this.partitions == Int.MinValue)
      errors += s"'Partitions' is required"
    else {
      if (this.partitions <= 0)
        errors += s"'Partitions' must be a positive integer"
    }

    //replicationFactor
    if (this.replicationFactor == Int.MinValue)
      errors += s"'Replication-factor' is required"
    else {
      if (this.replicationFactor <= 0) {
        errors += s"'Replication-factor' must be a positive integer"
      }
    }

    errors
  }

  override def asModelStream() = {
    val modelStream = new KafkaSjStream()
    super.fillModelStream(modelStream)
    modelStream.partitions = this.partitions
    modelStream.replicationFactor = this.replicationFactor

    modelStream
  }

  private def checkStreamPartitionsOnConsistency(service: KafkaService) = {
    val errors = new ArrayBuffer[String]()
    val zkUtils = createZkUtils()
    val topicMetadata = AdminUtils.fetchTopicMetadataFromZk(this.name, zkUtils)
    if (topicMetadata.partitionsMetadata.size != this.partitions) {
      errors += s"Partitions count of stream '${this.name}' mismatch. Kafka stream partitions (${this.partitions}) " +
        s"mismatch partitions of exists kafka topic (${topicMetadata.partitionsMetadata.size})"
    }

    errors
  }

  override def create() = {
    try {
      val zkUtils = createZkUtils()
      if (doesStreamHaveForcedCreation(zkUtils)) {
        deleteTopic(zkUtils)
        createTopic(zkUtils)
      } else {
        if (!doesTopicExist(zkUtils)) createTopic(zkUtils)
      }
    } catch {
      case ex: TopicAlreadyMarkedForDeletionException =>
        throw new Exception(s"Cannot delete a kafka topic '${this.name}'. Topic is already marked for deletion. It means that kafka doesn't support deletion")
      case e: TopicExistsException =>
        throw new Exception(s"Cannot create a kafka topic '${this.name}'. Topic is marked for deletion. It means that kafka doesn't support deletion")
    }
  }

  private def createZkUtils() = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val service = serviceDAO.get(this.service).get.asInstanceOf[KafkaService]
    val zkHost = service.zkProvider.hosts
    val zkConnect = new ZkConnection(zkHost.mkString(";"))
    val zkTimeout = ConfigSettingsUtils.getZkSessionTimeout()
    val zkClient = ZkUtils.createZkClient(zkHost.mkString(";"), zkTimeout, zkTimeout)
    val zkUtils = new ZkUtils(zkClient, zkConnect, false)

    zkUtils
  }

  private def doesStreamHaveForcedCreation(zkUtils: ZkUtils) = {
    doesTopicExist(zkUtils) && this.force
  }

  private def doesTopicExist(zkUtils: ZkUtils) = {
    AdminUtils.topicExists(zkUtils, this.name)
  }

  private def deleteTopic(zkUtils: ZkUtils) = AdminUtils.deleteTopic(zkUtils, this.name)


  private def createTopic(zkUtils: ZkUtils) = {
    AdminUtils.createTopic(zkUtils, this.name, this.partitions, this.replicationFactor, new Properties())
  }
}
