package com.bwsw.sj.common.si.model.stream

import java.util.Properties

import com.bwsw.sj.common.config.SettingsUtils
import com.bwsw.sj.common.dal.model.service.KafkaServiceDomain
import com.bwsw.sj.common.dal.model.stream.KafkaStreamDomain
import com.bwsw.sj.common.utils.{ServiceLiterals, StreamLiterals}
import kafka.admin.AdminUtils
import kafka.common.TopicAlreadyMarkedForDeletionException
import kafka.utils.ZkUtils
import org.I0Itec.zkclient.ZkConnection
import org.apache.kafka.common.errors.TopicExistsException
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

class KafkaStream(name: String,
                  service: String,
                  val partitions: Int,
                  val replicationFactor: Int,
                  tags: Array[String],
                  force: Boolean,
                  streamType: String,
                  description: String)
                 (implicit injector: Injector)
  extends SjStream(streamType, name, service, tags, force, description) {

  import messageResourceUtils.createMessage

  private val settingsUtils = inject[SettingsUtils]

  override def to(): KafkaStreamDomain = {
    val serviceRepository = connectionRepository.getServiceRepository

    new KafkaStreamDomain(
      name,
      serviceRepository.get(service).get.asInstanceOf[KafkaServiceDomain],
      partitions,
      replicationFactor,
      description,
      force,
      tags)
  }

  override def create(): Unit = {
    Try {
      val zkUtils = createZkUtils()
      if (doesStreamHaveForcedCreation(zkUtils)) {
        deleteTopic(zkUtils)
        createTopic(zkUtils)
      } else {
        if (!doesTopicExist(zkUtils)) createTopic(zkUtils)
      }
    } match {
      case Success(_) =>
      case Failure(_: TopicAlreadyMarkedForDeletionException) =>
        throw new Exception(s"Cannot delete a kafka topic '$name'. Topic is already marked for deletion. It means that kafka doesn't support deletion")
      case Failure(_: TopicExistsException) =>
        throw new Exception(s"Cannot create a kafka topic '$name'. Topic is marked for deletion. It means that kafka doesn't support deletion")
      case Failure(e) => throw e
    }
  }

  override def delete(): Unit = {
    Try {
      val zkUtils = createZkUtils()
      if (doesTopicExist(zkUtils)) {
        deleteTopic(zkUtils)
      }
    } match {
      case Success(_) =>
      case Failure(_: TopicAlreadyMarkedForDeletionException) =>
        throw new Exception(s"Cannot delete a kafka topic '$name'. Topic is already marked for deletion. It means that kafka doesn't support deletion")
      case Failure(e) => throw e
    }
  }

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    errors ++= super.validateGeneralFields()

    //partitions
    if (partitions <= 0)
      errors += createMessage("entity.error.attribute.required", "Partitions") + ". " +
        createMessage("entity.error.attribute.must.be.positive.integer", "Partitions")

    //replicationFactor
    if (replicationFactor <= 0) {
      errors += createMessage("entity.error.attribute.required", "replicationFactor") + ". " +
        createMessage("entity.error.attribute.must.be.positive.integer", "replicationFactor")
    }

    Option(service) match {
      case Some("") | None =>
        errors += createMessage("entity.error.attribute.required", "Service")

      case Some(x) =>
        val serviceDAO = connectionRepository.getServiceRepository
        val serviceObj = serviceDAO.get(x)
        serviceObj match {
          case None =>
            errors += createMessage("entity.error.doesnot.exist", "Service", x)
          case Some(someService) =>
            if (someService.serviceType != ServiceLiterals.kafkaType) {
              errors += createMessage("entity.error.must.one.type.other.given",
                s"Service for '${StreamLiterals.kafkaStreamType}' stream",
                ServiceLiterals.kafkaType,
                someService.serviceType)
            } else {
              if (errors.isEmpty)
                errors ++= checkStreamPartitionsOnConsistency(someService.asInstanceOf[KafkaServiceDomain])
            }
        }
    }

    errors
  }

  private def checkStreamPartitionsOnConsistency(service: KafkaServiceDomain): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    val zkUtils = createZkUtils()
    val topicMetadata = AdminUtils.fetchTopicMetadataFromZk(name, zkUtils)
    if (!topicMetadata.partitionMetadata().isEmpty && topicMetadata.partitionMetadata().size != partitions) {
      errors += createMessage("entity.error.mismatch.partitions", name, s"$partitions", s"${topicMetadata.partitionMetadata().size}")
    }

    errors
  }

  private def createZkUtils(): ZkUtils = {
    val serviceDAO = connectionRepository.getServiceRepository
    val service = serviceDAO.get(this.service).get.asInstanceOf[KafkaServiceDomain]
    val zkHost = service.zkProvider.hosts
    val zkConnect = new ZkConnection(zkHost.mkString(";"))
    val zkTimeout = settingsUtils.getZkSessionTimeout()
    val zkClient = ZkUtils.createZkClient(zkHost.mkString(";"), zkTimeout, zkTimeout)
    val zkUtils = new ZkUtils(zkClient, zkConnect, false)

    zkUtils
  }

  private def doesStreamHaveForcedCreation(zkUtils: ZkUtils): Boolean =
    doesTopicExist(zkUtils) && force

  private def doesTopicExist(zkUtils: ZkUtils): Boolean =
    AdminUtils.topicExists(zkUtils, name)

  private def deleteTopic(zkUtils: ZkUtils): Unit =
    AdminUtils.deleteTopic(zkUtils, name)

  private def createTopic(zkUtils: ZkUtils): Unit =
    AdminUtils.createTopic(zkUtils, name, partitions, replicationFactor, new Properties())
}
