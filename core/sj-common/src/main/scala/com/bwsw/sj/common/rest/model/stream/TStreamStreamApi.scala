package com.bwsw.sj.common.rest.model.stream

import com.bwsw.sj.common.dal.model.service.TStreamServiceDomain
import com.bwsw.sj.common.dal.model.stream.TStreamStreamDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.stream.TStreamStream
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils._
import com.bwsw.tstreams.common.StorageClient
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}
import com.bwsw.tstreams.streams.Stream

import scala.collection.mutable.ArrayBuffer

class TStreamStreamApi(override val name: String,
                       override val service: String,
                       override val tags: Array[String] = Array(),
                       override val force: Boolean = false,
                       override val description: String = RestLiterals.defaultDescription,
                       val partitions: Int = Int.MinValue)
  extends StreamApi(StreamLiterals.tstreamType, name, service, tags, force, description) {

  override def to: TStreamStream =
    new TStreamStream(name, service, partitions, tags, force, streamType, description)

  override def validate(): ArrayBuffer[String] = {
    val serviceDAO = ConnectionRepository.getServiceRepository
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()


    //partitions
    if (this.partitions <= 0)
      errors += createMessage("entity.error.attribute.required", "Partitions") + ". " +
        createMessage("entity.error.attribute.must.be.positive.integer", "Partitions")

    Option(this.service) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Service")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Service")
        }
        else {
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
                if (errors.isEmpty) errors ++= checkStreamPartitionsOnConsistency(someService.asInstanceOf[TStreamServiceDomain])
              }
          }
        }
    }

    errors
  }

  override def asModelStream(): TStreamStreamDomain = {
    val serviceDAO = ConnectionRepository.getServiceRepository
    val modelStream = new TStreamStreamDomain(
      this.name,
      serviceDAO.get(this.service).get.asInstanceOf[TStreamServiceDomain],
      this.partitions,
      this.description,
      this.force,
      this.tags
    )

    modelStream
  }

  private def checkStreamPartitionsOnConsistency(service: TStreamServiceDomain): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    val tstreamFactory = new TStreamsFactory()
    tstreamFactory.setProperty(ConfigurationOptions.StorageClient.Auth.key, service.token)
      .setProperty(ConfigurationOptions.Coordination.endpoints, service.provider.hosts.mkString(","))
      .setProperty(ConfigurationOptions.StorageClient.Zookeeper.endpoints, service.provider.hosts.mkString(","))
      .setProperty(ConfigurationOptions.StorageClient.Zookeeper.prefix, service.prefix)

    val storageClient = tstreamFactory.getStorageClient()

    if (storageClient.checkStreamExists(this.name) && !this.force) {
      val tStream = storageClient.loadStream(this.name)
      if (tStream.partitionsCount != this.partitions) {
        errors += createMessage("entity.error.mismatch.partitions", this.name, s"${this.partitions}", s"${tStream.partitionsCount}")
      }
    }

    storageClient.shutdown()

    errors
  }

  override def create(): Unit = {
    val serviceDAO = ConnectionRepository.getServiceRepository
    val service = serviceDAO.get(this.service).get.asInstanceOf[TStreamServiceDomain]
    val tstreamFactory = new TStreamsFactory()
    tstreamFactory.setProperty(ConfigurationOptions.StorageClient.Auth.key, service.token)
      .setProperty(ConfigurationOptions.Coordination.endpoints, service.provider.hosts.mkString(","))
      .setProperty(ConfigurationOptions.StorageClient.Zookeeper.endpoints, service.provider.hosts.mkString(","))
      .setProperty(ConfigurationOptions.StorageClient.Zookeeper.prefix, service.prefix)

    val storageClient = tstreamFactory.getStorageClient()
    if (doesStreamHaveForcedCreation(storageClient)) {
      storageClient.deleteStream(this.name)
      createTStream(storageClient)
    } else {
      if (!doesTopicExist(storageClient)) createTStream(storageClient)
    }

    storageClient.shutdown()
  }

  private def doesStreamHaveForcedCreation(storageClient: StorageClient): Boolean = {
    doesTopicExist(storageClient) && this.force
  }

  private def doesTopicExist(storageClient: StorageClient): Boolean = {
    storageClient.checkStreamExists(this.name)
  }

  private def createTStream(storageClient: StorageClient): Stream = {
    storageClient.createStream(
      this.name,
      this.partitions,
      StreamLiterals.ttl,
      this.description
    )
  }
}
