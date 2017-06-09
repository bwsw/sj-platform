package com.bwsw.sj.common.si.model.stream

import com.bwsw.common.KafkaClient
import com.bwsw.sj.common.config.SettingsUtils
import com.bwsw.sj.common.dal.model.service.KafkaServiceDomain
import com.bwsw.sj.common.dal.model.stream.KafkaStreamDomain
import com.bwsw.sj.common.utils.{ServiceLiterals, StreamLiterals}
import kafka.common.TopicAlreadyMarkedForDeletionException
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
      tags,
      settingsUtils.getZkSessionTimeout())
  }

  override def create(): Unit = {
    Try {
      val kafkaClient = createKafkaClient()
      if (doesStreamHaveForcedCreation(kafkaClient)) {
        deleteTopic(kafkaClient)
        createTopic(kafkaClient)
      } else {
        if (!doesTopicExist(kafkaClient)) createTopic(kafkaClient)
      }

      kafkaClient.close()
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
      val kafkaClient = createKafkaClient()
      if (doesTopicExist(kafkaClient)) {
        deleteTopic(kafkaClient)
      }

      kafkaClient.close()
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
    val kafkaClient = createKafkaClient()
    val topicMetadata = kafkaClient.fetchTopicMetadataFromZk(name)
    if (!topicMetadata.partitionMetadata().isEmpty && topicMetadata.partitionMetadata().size != partitions) {
      errors += createMessage("entity.error.mismatch.partitions", name, s"$partitions", s"${topicMetadata.partitionMetadata().size}")
    }

    errors
  }

  private def createKafkaClient(): KafkaClient = {
    val serviceDAO = connectionRepository.getServiceRepository
    val service = serviceDAO.get(this.service).get.asInstanceOf[KafkaServiceDomain]
    val zkServers = service.zkProvider.hosts

    new KafkaClient(zkServers, settingsUtils.getZkSessionTimeout())
  }

  private def doesStreamHaveForcedCreation(kafkaClient: KafkaClient): Boolean =
    doesTopicExist(kafkaClient) && force

  private def doesTopicExist(kafkaClient: KafkaClient): Boolean =
    kafkaClient.topicExists(name)

  private def deleteTopic(kafkaClient: KafkaClient): Unit =
    kafkaClient.deleteTopic(name)

  private def createTopic(kafkaClient: KafkaClient): Unit =
    kafkaClient.createTopic(name, partitions, replicationFactor)
}
