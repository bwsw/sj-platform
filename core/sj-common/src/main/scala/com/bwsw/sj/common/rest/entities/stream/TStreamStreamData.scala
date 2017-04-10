package com.bwsw.sj.common.rest.entities.stream

import com.bwsw.sj.common.DAL.model.{TStreamService, TStreamSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils._
import com.bwsw.tstreams.data.IStorage
import com.bwsw.tstreams.metadata.MetadataStorage
import com.bwsw.tstreams.streams.StreamService

import scala.collection.mutable.ArrayBuffer

class TStreamStreamData() extends StreamData() {
  streamType = StreamLiterals.tstreamType
  var partitions: Int = Int.MinValue

  override def validate() = {
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

  override def asModelStream() = {
    val modelStream = new TStreamSjStream()
    super.fillModelStream(modelStream)
    modelStream.partitions = this.partitions

    modelStream
  }

  private def checkStreamPartitionsOnConsistency(service: TStreamService) = {
    val errors = new ArrayBuffer[String]()
//    val metadataStorage = createMetadataStorage(service)
//    val dataStorage = createDataStorage(service)
//
//    if (StreamService.isExist(this.name, metadataStorage) && !this.force) {
//      val tStream = StreamService.loadStream[Array[Byte]](
//        this.name,
//        metadataStorage,
//        dataStorage
//      )
//      if (tStream.partitionsCount != this.partitions) {
//        errors += createMessage("entity.error.mismatch.partitions", this.name, s"${this.partitions}", s"${tStream.partitionsCount}")
//      }
//    }

    errors
  }

  override def create() = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val service = serviceDAO.get(this.service).get.asInstanceOf[TStreamService]
//    val metadataStorage = createMetadataStorage(service)
    //    val dataStorage = createDataStorage(service)
    //
    //    if (doesStreamHaveForcedCreation(metadataStorage)) {
    //      deleteStream(metadataStorage)
    //      createTStream(metadataStorage, dataStorage)
    //    } else {
    //      if (!doesTopicExist(metadataStorage)) createTStream(metadataStorage, dataStorage)
    //    }
  }

  private def doesStreamHaveForcedCreation(metadataStorage: MetadataStorage) = {
    doesTopicExist(metadataStorage) && this.force
  }

  private def doesTopicExist(metadataStorage: MetadataStorage) = {
    StreamService.isExist(this.name, metadataStorage)
  }

  private def deleteStream(metadataStorage: MetadataStorage) = StreamService.deleteStream(this.name, metadataStorage)

  private def createTStream(metadataStorage: MetadataStorage,
                            dataStorage: IStorage[Array[Byte]]) = {
    StreamService.createStream(
      this.name,
      this.partitions,
      StreamLiterals.ttl,
      this.description,
      metadataStorage,
      dataStorage
    )
  }
}
