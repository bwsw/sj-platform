package com.bwsw.sj.crud.rest.validator.stream

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.StreamConstants
import com.bwsw.sj.crud.rest.entities._
import com.bwsw.sj.crud.rest.entities.stream.{KafkaSjStreamData, TStreamSjStreamData, SjStreamData}
import com.bwsw.sj.crud.rest.validator.provider.ProviderValidator
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
  * Created by mendelbaum_nm
  */
object StreamValidator {

  private val logger = LoggerFactory.getLogger(getClass.getName)

  /**
    * Validating input parameters for stream
    *
    * @param stream - parameters of stream being validated
    * @param initialData - input parameters for stream being validated
    * @return - List of errors
    */
  def validate(initialData: SjStreamData, stream: SjStream) = {
    logger.debug(s"Stream ${initialData.name}. Start stream validation.")

    val streamDAO = ConnectionRepository.getStreamService
    val serviceDAO = ConnectionRepository.getServiceManager

    val errors = new ArrayBuffer[String]()

    Option(initialData.name) match {
      case None =>
        errors += s"'name' is required"
      case Some(x) =>
        if (x.isEmpty)
          errors += s"'name' can not be empty"
    }

    if (!initialData.name.matches("""^([a-zA-Z][a-zA-Z0-9-]+)$""")) {
      errors += s"Stream has incorrect name: ${initialData.name}. Name of stream must be contain digits, letters or hyphens. First symbol must be letter."
    }

    val streamObj = streamDAO.get(initialData.name)
    if (streamObj != null) {
      errors += s"Stream with name ${streamObj.name} already exists"
    }

    Option(initialData.description) match {
      case None =>
        errors += s"'description' is required"
      case Some(d) =>
        if (d.isEmpty)
          errors += s"'description' can not be empty"
    }


    var serviceObj: Service = null
    Option(initialData.service) match {
      case None =>
        errors += s"'service' is required"
      case Some(x) =>
        if (x.isEmpty)
          errors += s"'service' can not be empty"
        else {
          serviceObj = serviceDAO.get(initialData.service)
          if (serviceObj == null) {
            errors += s"Service '${initialData.service}' does not exist"
          }
          else if (initialData.streamType == StreamConstants.tStream && serviceObj.serviceType != "TstrQ") {
            errors += s"Service for ${StreamConstants.tStream} stream must be of TstrQ type. '${serviceObj.name}' is not of type TstrQ"
          } else if (initialData.streamType == StreamConstants.kafka && serviceObj.serviceType != "KfkQ") {
            errors += s"Service for '${StreamConstants.kafka}' stream must be of KfkQ type. '${serviceObj.name}' is not of type KfkQ"
          } else if (initialData.streamType == StreamConstants.esOutput && serviceObj.serviceType != "ESInd") {
            errors += s"Service for '${StreamConstants.esOutput}' stream must be of ESInd type. '${serviceObj.name}' is not of type ESInd"
          } else if (initialData.streamType == StreamConstants.jdbcOutput && serviceObj.serviceType != "JDBC") {
            errors += s"Service for '${StreamConstants.jdbcOutput}' stream must be of JDBC type. '${serviceObj.name}' is not of type JDBC"
          }
        }
    }


    Option(initialData.streamType) match {
      case Some(t) if !StreamConstants.streamTypes.contains(t) =>
        errors += s"Unknown 'stream-type' provided. Must be one of: ${StreamConstants.streamTypes}"
      case None =>
        errors += s"'stream-type' is required"
      case _ =>
    }

    Option(initialData.tags) match {
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
    initialData.streamType match {
      case StreamConstants.tStream =>
        //partitions
        if (initialData.asInstanceOf[TStreamSjStreamData].partitions <= 0)
          errors += s"'partitions' is required and must be a positive integer"
        else
          stream.asInstanceOf[TStreamSjStream].partitions = initialData.asInstanceOf[TStreamSjStreamData].partitions

        //generator
        if (initialData.asInstanceOf[TStreamSjStreamData].generator == null) {
          errors += s"'generator' is required for '${StreamConstants.tStream}'-type stream."
        }
        else {
          val generator = new Generator
          val generatorErrors = GeneratorValidator.validate(
            initialData.asInstanceOf[TStreamSjStreamData].generator,
            generator
          )
          errors ++= generatorErrors
          if (generatorErrors.isEmpty)
            stream.asInstanceOf[TStreamSjStream].generator = generator
        }

      case StreamConstants.kafka =>
        //partitions
        if (initialData.asInstanceOf[KafkaSjStreamData].partitions <= 0)
          errors += s"'partitions' is required and must be a positive integer"
        else
          stream.asInstanceOf[KafkaSjStream].partitions = initialData.asInstanceOf[KafkaSjStreamData].partitions

        //replicationFactor
        val rFactor = initialData.asInstanceOf[KafkaSjStreamData].replicationFactor
        if (rFactor <= 0) {
          errors += s"'replication-factor' is required for '${StreamConstants.kafka}' stream and must be a positive integer"
        }
        else {
          if (serviceObj != null) {
            val zkHostsNumber = serviceObj.asInstanceOf[KafkaService].zkProvider.hosts.length

            if (rFactor > zkHostsNumber) {
              errors += s"'replication-factor' can not be greater than service's zk-provider hosts number"
            }
            else {
              val zkHostsConnectionErrors = ProviderValidator.checkProviderConnection(serviceObj.asInstanceOf[KafkaService].zkProvider)
              if (rFactor > (zkHostsNumber - zkHostsConnectionErrors.length)) {
                errors += s"Some service's zk-provider hosts are unreachable. There is not enough alive hosts for 'replication-factor' provided"
                errors ++= zkHostsConnectionErrors
              }
              else {
                stream.asInstanceOf[KafkaSjStream].replicationFactor = rFactor
              }
            }
          }
          else {
            // the error will occur on service check above, nothing to do here
          }
        }

      case _ =>
    }

    // Fulfill common fields
    if (errors.isEmpty) {
      stream.name = initialData.name
      stream.description = initialData.description
      stream.service = serviceObj
      stream.tags = initialData.tags
      stream.streamType = initialData.streamType
    }

    errors
  }
}
