package com.bwsw.sj.common.si.model.stream

import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.bwsw.sj.common.dal.model.service.KafkaServiceDomain
import com.bwsw.sj.common.dal.model.stream.KafkaStreamDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage
import com.bwsw.sj.common.utils.{ServiceLiterals, StreamLiterals}
import kafka.admin.AdminUtils
import kafka.utils.ZkUtils
import org.I0Itec.zkclient.ZkConnection

import scala.collection.mutable.ArrayBuffer

class KafkaStream(name: String,
                  service: String,
                  val partitions: Int,
                  val replicationFactor: Int,
                  tags: Array[String],
                  force: Boolean,
                  streamType: String,
                  description: String)
  extends SjStream(streamType, name, service, tags, force, description) {

  override def to(): KafkaStreamDomain = {
    val serviceRepository = ConnectionRepository.getServiceRepository

    new KafkaStreamDomain(
      name,
      serviceRepository.get(service).get.asInstanceOf[KafkaServiceDomain],
      partitions,
      replicationFactor,
      description,
      force,
      tags)
  }

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    errors ++= super.validateGeneralFields()

    //partitions
    if (this.partitions <= 0)
      errors += createMessage("entity.error.attribute.required", "Partitions") + ". " +
        createMessage("entity.error.attribute.must.be.positive.integer", "Partitions")


    Option(this.service) match {
      case Some("") | None =>
        errors += createMessage("entity.error.attribute.required", "Service")

      case Some(x) =>
        val serviceDAO = ConnectionRepository.getServiceRepository
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
    val serviceDAO = ConnectionRepository.getServiceRepository
    val service = serviceDAO.get(this.service).get.asInstanceOf[KafkaServiceDomain]
    val zkHost = service.zkProvider.hosts
    val zkConnect = new ZkConnection(zkHost.mkString(";"))
    val zkTimeout = ConfigurationSettingsUtils.getZkSessionTimeout()
    val zkClient = ZkUtils.createZkClient(zkHost.mkString(";"), zkTimeout, zkTimeout)
    val zkUtils = new ZkUtils(zkClient, zkConnect, false)

    zkUtils
  }
}
