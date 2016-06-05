package com.bwsw.sj.crud.rest.validator.stream

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.StreamConstants
import com.bwsw.sj.crud.rest.entities._
import com.bwsw.sj.crud.rest.validator.provider.ProviderValidator

import scala.collection.mutable.ArrayBuffer

/**
  * Created by mendelbaum_nm
  */
object StreamValidator {

  var streamDAO: GenericMongoService[SjStream] = null
  var providerDAO: GenericMongoService[Provider] = null

  /**
    * Validating input parameters for stream
    *
    * @param params - parameters of stream being validated
    * @param initialData - input parameters for stream being validated
    * @return - List of errors
    */
  def validate(params: SjStream, initialData: SjStreamData) = {
    streamDAO = ConnectionRepository.getStreamService
    providerDAO = ConnectionRepository.getProviderService

    val errors = new ArrayBuffer[String]()

    Option(params.name) match {
      case Some(n) if n.isEmpty =>
        errors += s"'name' can not be empty"
      case None =>
        errors += s"'name' is required"
      case _ =>
    }

    val stream = streamDAO.get(params.name)
    if (stream != null) {
      errors += s"Stream with name ${params.name} already exists"
    }

    Option(params.description) match {
      case Some(d) if d.isEmpty =>
        errors += s"'description' can not be empty"
      case None =>
        errors += s"'description' is required"
      case _ =>
    }

    Option(initialData.service) match {
      case Some(s) if s.isEmpty =>
        errors += s"'service' can not be empty"
      case Some(s) if params.service == null =>
        errors += s"Unknown 'service' provided"
      case None =>
        errors += s"'service' is required"
      case _ =>
        if (params.streamType == StreamConstants.tStream && params.service.serviceType != "TstrQ") {
          errors += s"Service for 'stream.t-stream' stream must be of TstrQ type. '${params.service.name}' is not of type TstrQ"
        } else if (params.streamType == StreamConstants.kafka && params.service.serviceType != "KfkQ") {
            errors += s"Service for 'stream.kafka' stream must be of KfkQ type. '${params.service.name}' is not of type KfkQ"
        } else if (params.streamType == StreamConstants.esOutput && params.service.serviceType != "ESInd") {
          errors += s"Service for 'elasticsearch-output' stream must be of ESInd type. '${params.service.name}' is not of type ESInd"
        } else if (params.streamType == StreamConstants.jdbcOutput && params.service.serviceType != "JDBC") {
          errors += s"Service for 'jdbc-output' stream must be of JDBC type. '${params.service.name}' is not of type JDBC"
        }
    }

    Option(params.streamType) match {
      case Some(t) if !StreamConstants.streamTypes.contains(t) =>
        errors += s"Unknown 'stream-type' provided. Must be one of: ${StreamConstants.streamTypes}"
      case None =>
        errors += s"'stream-type' is required"
      case _ =>
    }

    Option(params.tags) match {
      case Some(t) =>
        if (t.isEmpty)
          errors += s"'tags' can not be empty"
        else if (t.contains(""))
          errors += s"Tag in 'tags' can not be empty string"
      case None =>
        errors += s"'tags' is required"
      case _ =>
    }

    // streamType-specific validations
    params.streamType match {
      case StreamConstants.tStream =>
        //partitions
        if (params.asInstanceOf[TStreamSjStream].partitions <= 0) {
          errors += s"'partitions' is required and must be a positive integer"
        }

        //generator
        errors ++= GeneratorValidator.validate(
          params.asInstanceOf[TStreamSjStream].generator,
          initialData.asInstanceOf[TStreamSjStreamData].generator
        )
      case StreamConstants.kafka =>
        //partitions
        if (params.asInstanceOf[KafkaSjStream].partitions <= 0) {
          errors += s"'partitions' is required and must be a positive integer"
        }

        //replicationFactor
        val rFactor = params.asInstanceOf[KafkaSjStream].replicationFactor
        if (rFactor <= 0) {
          errors += s"'replication-factor' is required for '${StreamConstants.kafka}' stream and must be a positive integer"
        }
        else {
          val zkHostsNumber = params.service.asInstanceOf[KafkaService].zkProvider.hosts.length

          if (rFactor > zkHostsNumber) {
            errors += s"'replication-factor' can not be greater than service's zk-provider hosts number"
          }
          else {
            val zkHostsConnectionErrors = ProviderValidator.checkProviderConnection(params.service.asInstanceOf[KafkaService].zkProvider)
            if (rFactor > (zkHostsNumber - zkHostsConnectionErrors.length)) {
              errors += s"Some service's zk-provider hosts are unreachable. There is not enough alive hosts for 'replication-factor' provided"
              errors ++= zkHostsConnectionErrors

            }
          }
        }

      case _ =>
    }

    errors
  }
}
