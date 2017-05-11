package com.bwsw.sj.common.si.model.stream

import com.bwsw.sj.common.dal.model.service.TStreamServiceDomain
import com.bwsw.sj.common.dal.model.stream.TStreamStreamDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage
import com.bwsw.sj.common.utils.{ServiceLiterals, StreamLiterals}
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}

import scala.collection.mutable.ArrayBuffer

class TStreamStream(name: String,
                    service: String,
                    val partitions: Int,
                    tags: Array[String],
                    force: Boolean,
                    streamType: String,
                    description: String)
  extends SjStream(streamType, name, service, tags, force, description) {

  override def to(): TStreamStreamDomain = {
    val serviceRepository = ConnectionRepository.getServiceRepository

    new TStreamStreamDomain(
      name,
      serviceRepository.get(service).get.asInstanceOf[TStreamServiceDomain],
      partitions,
      description,
      force,
      tags)
  }

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    errors ++= super.validateGeneralFields()

    //partitions
    if (partitions <= 0)
      errors += createMessage("entity.error.attribute.required", "Partitions") + ". " +
        createMessage("entity.error.attribute.must.be.positive.integer", "Partitions")

    Option(service) match {
      case Some("") | None =>
        errors += createMessage("entity.error.attribute.required", "Service")
      case Some(x) =>
        val serviceDAO = ConnectionRepository.getServiceRepository
        val serviceObj = serviceDAO.get(x)
        serviceObj match {
          case None =>
            errors += createMessage("entity.error.doesnot.exist", "Service", x)
          case Some(someService) =>
            if (someService.serviceType != ServiceLiterals.tstreamsType) {
              errors += createMessage("entity.error.must.one.type.other.given",
                s"Service for '${StreamLiterals.tstreamType}' stream",
                ServiceLiterals.tstreamsType,
                someService.serviceType)
            } else {
              if (errors.isEmpty) {
                errors ++= checkStreamPartitionsOnConsistency(someService.asInstanceOf[TStreamServiceDomain])
              }
            }
        }
    }

    errors
  }

  private def checkStreamPartitionsOnConsistency(service: TStreamServiceDomain): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    val tstreamFactory = new TStreamsFactory()
    tstreamFactory.setProperty(ConfigurationOptions.StorageClient.Auth.key, service.token)
      .setProperty(ConfigurationOptions.Coordination.endpoints, service.provider.hosts.mkString(","))
      .setProperty(ConfigurationOptions.StorageClient.Zookeeper.endpoints, service.provider.hosts.mkString(","))
      .setProperty(ConfigurationOptions.StorageClient.Zookeeper.prefix, service.prefix)

    val storageClient = tstreamFactory.getStorageClient()

    if (storageClient.checkStreamExists(name) && !force) {
      val tStream = storageClient.loadStream(name)
      if (tStream.partitionsCount != partitions) {
        errors += createMessage("entity.error.mismatch.partitions", name, s"$partitions", s"${tStream.partitionsCount}")
      }
    }

    storageClient.shutdown()

    errors
  }
}
