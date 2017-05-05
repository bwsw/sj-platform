package com.bwsw.sj.common.rest.entities.stream

import com.bwsw.sj.common.DAL.model.service.TStreamService
import com.bwsw.sj.common.DAL.model.stream.TStreamSjStream
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils._
import com.bwsw.tstreams.common.StorageClient
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}
import com.bwsw.tstreams.streams.Stream

import scala.collection.mutable.ArrayBuffer

class TStreamStreamData() extends StreamData() {
  streamType = StreamLiterals.tstreamType
  var partitions: Int = Int.MinValue

  override def validate(): ArrayBuffer[String] = {
    val serviceDAO = ConnectionRepository.getServiceManager
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
                if (errors.isEmpty) errors ++= checkStreamPartitionsOnConsistency(someService.asInstanceOf[TStreamService])
              }
          }
        }
    }

    errors
  }

  override def asModelStream(): TStreamSjStream = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val modelStream = new TStreamSjStream(
      this.name,
      serviceDAO.get(this.service).get.asInstanceOf[TStreamService],
      this.partitions,
      this.description,
      this.force,
      this.tags
    )

    modelStream
  }

  private def checkStreamPartitionsOnConsistency(service: TStreamService): ArrayBuffer[String] = {
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
    val serviceDAO = ConnectionRepository.getServiceManager
    val service = serviceDAO.get(this.service).get.asInstanceOf[TStreamService]
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
