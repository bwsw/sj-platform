package com.bwsw.sj.common.rest.entities.stream

import com.bwsw.sj.common.DAL.model.{TStreamService, TStreamSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils._
import com.bwsw.tstreams.data.IStorage
import com.bwsw.tstreams.metadata.MetadataStorage
import com.bwsw.tstreams.services.BasicStreamService
import SjStreamUtilsForCreation._

import scala.collection.mutable.ArrayBuffer

class TStreamSjStreamData() extends SjStreamData() {
  streamType = StreamLiterals.tStreamType
  var partitions: Int = Int.MinValue
  var generator: GeneratorData = new GeneratorData(GeneratorLiterals.localType)

  override def validate() = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()


    //partitions
    if (this.partitions <= 0)
      errors += "'Partitions' attribute is required" + ". " +
        s"'Partitions' must be a positive integer"

    Option(this.service) match {
      case None =>
        errors += s"'Service' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'Service' is required"
        }
        else {
          val serviceObj = serviceDAO.get(x)
          serviceObj match {
            case None =>
              errors += s"Service '$x' does not exist"
            case Some(modelService) =>
              if (modelService.serviceType != ServiceLiterals.tstreamsType) {
                errors += s"Service for '${StreamLiterals.tStreamType}' stream " +
                  s"must be of '${ServiceLiterals.tstreamsType}' type ('${modelService.serviceType}' is given instead)"
              } else {
                if (errors.isEmpty) errors ++= checkStreamPartitionsOnConsistency(modelService.asInstanceOf[TStreamService])
              }
          }
        }
    }

    //generator
    errors ++= this.generator.validate()

    errors
  }

  override def asModelStream() = {
    val modelStream = new TStreamSjStream()
    super.fillModelStream(modelStream)
    modelStream.partitions = this.partitions
    modelStream.generator = this.generator.asModelGenerator()

    modelStream
  }

  private def checkStreamPartitionsOnConsistency(service: TStreamService) = {
    val errors = new ArrayBuffer[String]()
    val metadataStorage = createMetadataStorage(service)
    val dataStorage = createDataStorage(service)

    if (BasicStreamService.isExist(this.name, metadataStorage) && !this.force) {
      val tStream = BasicStreamService.loadStream[Array[Byte]](
        this.name,
        metadataStorage,
        dataStorage
      )
      if (tStream.getPartitions != this.partitions) {
        errors += s"Partitions count of stream '${this.name}' mismatch. T-stream partitions (${this.partitions}) " +
          s"mismatch with partitions of existent t-stream (${tStream.getPartitions})"
      }
    }

    errors
  }

  override def create() = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val service = serviceDAO.get(this.service).get.asInstanceOf[TStreamService]
    val metadataStorage = createMetadataStorage(service)
    val dataStorage = createDataStorage(service)

    if (doesStreamHaveForcedCreation(metadataStorage)) {
      deleteStream(metadataStorage)
      createTStream(metadataStorage, dataStorage)
    } else {
      if (!doesTopicExist(metadataStorage)) createTStream(metadataStorage, dataStorage)
    }
  }

  private def doesStreamHaveForcedCreation(metadataStorage: MetadataStorage) = {
    doesTopicExist(metadataStorage) && this.force
  }

  private def doesTopicExist(metadataStorage: MetadataStorage) = {
    BasicStreamService.isExist(this.name, metadataStorage)
  }

  private def deleteStream(metadataStorage: MetadataStorage) = BasicStreamService.deleteStream(this.name, metadataStorage)

  private def createTStream(metadataStorage: MetadataStorage,
                            dataStorage: IStorage[Array[Byte]]) = {
    BasicStreamService.createStream(
      this.name,
      this.partitions,
      StreamLiterals.ttl,
      this.description,
      metadataStorage,
      dataStorage
    )
  }
}
