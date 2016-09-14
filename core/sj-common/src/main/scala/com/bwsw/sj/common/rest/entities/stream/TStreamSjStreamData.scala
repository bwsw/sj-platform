package com.bwsw.sj.common.rest.entities.stream

import java.net.InetSocketAddress

import com.aerospike.client.Host
import com.bwsw.sj.common.DAL.model.{TStreamService, TStreamSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{CassandraFactory, ProviderLiterals, ServiceLiterals, StreamLiterals}
import com.bwsw.tstreams.data.{IStorage, aerospike}
import com.bwsw.tstreams.metadata.MetadataStorage
import com.bwsw.tstreams.services.BasicStreamService

import scala.collection.mutable.ArrayBuffer

class TStreamSjStreamData() extends SjStreamData() {
  streamType = StreamLiterals.tStreamType
  var partitions: Int = 0
  var generator: GeneratorData = null

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
            if (modelService.serviceType != ServiceLiterals.tstreamsType) {
              errors += s"Service for ${StreamLiterals.tStreamType} stream " +
                s"must be of '${ServiceLiterals.tstreamsType}' type ('${modelService.serviceType}' is given instead)"
            } else {
              errors ++= checkStreamPartitionsOnConsistency(modelService.asInstanceOf[TStreamService])
            }
        }
    }

    //partitions
    if (this.partitions <= 0)
      errors += s"'Partitions' must be a positive integer"

    //generator
    if (this.generator == null) {
      errors += s"'Generator' is required"
    }
    else {
      errors ++= this.generator.validate()
    }

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

  private def createMetadataStorage(service: TStreamService) = {
    val metadataProvider = service.metadataProvider
    val hosts = metadataProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)).toSet
    val cassandraFactory = new CassandraFactory()
    cassandraFactory.open(hosts)
    val metadataStorage = cassandraFactory.getMetadataStorage(service.metadataNamespace)
    cassandraFactory.close()

    metadataStorage
  }

  private def createDataStorage(service: TStreamService) = {
    val dataProvider = service.dataProvider
    var dataStorage: IStorage[Array[Byte]] = null
    dataProvider.providerType match {
      case ProviderLiterals.cassandraType =>
        val hosts = dataProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)).toSet
        val cassandraFactory = new CassandraFactory()
        cassandraFactory.open(hosts)
        dataStorage = cassandraFactory.getDataStorage(service.dataNamespace)
        cassandraFactory.close()
      case ProviderLiterals.aerospikeType =>
        val options = new aerospike.Options(
          service.dataNamespace,
          dataProvider.hosts.map(s => new Host(s.split(":")(0), s.split(":")(1).toInt)).toSet
        )
        dataStorage = (new aerospike.Factory).getInstance(options) //todo
    }

    dataStorage
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
      createTStream(metadataStorage, dataStorage)
    }
  }

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
  
  private def doesStreamHaveForcedCreation(metadataStorage: MetadataStorage) = {
    BasicStreamService.isExist(this.name, metadataStorage) && this.force
  }
  
  private def deleteStream(metadataStorage: MetadataStorage) = BasicStreamService.deleteStream(this.name, metadataStorage)
}
