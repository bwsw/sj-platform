package com.bwsw.sj.crud.rest.validator.stream

import java.text.MessageFormat
import java.util.ResourceBundle

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.entities.stream.{KafkaSjStreamData, SjStreamData, TStreamSjStreamData}
import com.bwsw.sj.common.rest.utils.ValidationUtils
import com.bwsw.sj.common.utils.StreamConstants
import com.bwsw.sj.crud.rest.utils.StreamUtil
import kafka.common.TopicExistsException
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

object StreamValidator extends ValidationUtils {

  private val logger = LoggerFactory.getLogger(getClass.getName)

  /**
   * Validating input parameters for stream
   *
   * @param stream - parameters of stream being validated
   * @param initialData - input parameters for stream being validated
   * @return - List of errors
   */
  def validate(initialData: SjStreamData, stream: SjStream): ArrayBuffer[String] = {
    logger.debug(s"Stream ${initialData.name}. Start stream validation.")

    val streamDAO = ConnectionRepository.getStreamService
    val serviceDAO = ConnectionRepository.getServiceManager

    val errors = new ArrayBuffer[String]()

    Option(initialData.name) match {
      case None =>
        errors += s"'Name' is required"
      case Some(x) =>
        val streamObj = streamDAO.get(x)
        if (streamObj.isDefined) {
          errors += s"Stream with name $x already exists"
        }

        if (!validateName(x)) {
          errors += s"Stream has incorrect name: '$x'. " +
            s"Name of stream must be contain digits, lowercase letters or hyphens. First symbol must be a letter"
        }
    }

    Option(initialData.streamType) match {
      case Some(t) =>
        if (!StreamConstants.streamTypes.contains(t)) {
          errors += s"Unknown type '$t' provided. Must be one of: ${StreamConstants.streamTypes.mkString("[", ", ", "]")}"
        }
      case None =>
        errors += s"'Type' is required"
    }

    var serviceObj: Option[Service] = None
    Option(initialData.service) match {
      case None =>
        errors += s"'Service' is required"
      case Some(x) =>
        serviceObj = serviceDAO.get(initialData.service)
        serviceObj match {
          case None =>
            errors += s"Service '${initialData.service}' does not exist"
          case Some(service) =>
            if (initialData.streamType == StreamConstants.tStreamType && service.serviceType != "TstrQ") {
              errors += s"Service for ${StreamConstants.tStreamType} stream must be of 'TstrQ' type ('${service.serviceType}' is given instead)"
            } else if (initialData.streamType == StreamConstants.kafkaStreamType && service.serviceType != "KfkQ") {
              errors += s"Service for '${StreamConstants.kafkaStreamType}' stream must be of 'KfkQ' type ('${service.serviceType}' is given instead)"
            } else if (initialData.streamType == StreamConstants.esOutputType && service.serviceType != "ESInd") {
              errors += s"Service for '${StreamConstants.esOutputType}' stream must be of 'ESInd' type ('${service.serviceType}' is given instead)"
            } else if (initialData.streamType == StreamConstants.jdbcOutputType && service.serviceType != "JDBC") {
              errors += s"Service for '${StreamConstants.jdbcOutputType}' stream must be of 'JDBC' type ('${service.serviceType}' is given instead)"
            }
        }
    }

    // streamType-specific validations
    initialData.streamType match {
      case StreamConstants.`tStreamType` =>
        //partitions
        if (initialData.asInstanceOf[TStreamSjStreamData].partitions <= 0)
          errors += s"'Partitions' must be a positive integer"
        else
          stream.asInstanceOf[TStreamSjStream].partitions = initialData.asInstanceOf[TStreamSjStreamData].partitions

        //generator
        if (initialData.asInstanceOf[TStreamSjStreamData].generator == null) {
          errors += s"'Generator' is required"
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

      case StreamConstants.kafkaStreamType =>
        //partitions
        if (initialData.asInstanceOf[KafkaSjStreamData].partitions <= 0)
          errors += s"'Partitions' must be a positive integer"
        else
          stream.asInstanceOf[KafkaSjStream].partitions = initialData.asInstanceOf[KafkaSjStreamData].partitions

        //replicationFactor
        val rFactor = initialData.asInstanceOf[KafkaSjStreamData].replicationFactor
        if (rFactor <= 0) {
          errors += s"'Replication-factor' must be a positive integer"
        }
    }

    // Fulfill common fields
    if (errors.isEmpty) {
      stream.name = initialData.name
      stream.description = initialData.description
      stream.service = serviceObj.get
      stream.tags = initialData.tags
      stream.streamType = initialData.streamType

      stream match {
        case s: TStreamSjStream =>
          val streamCheckResult = StreamUtil.checkAndCreateTStream(s, initialData.force)
          streamCheckResult match {
            case Left(err) => errors += err
            case _ =>
          }
        case s: KafkaSjStream =>
          try {
            val streamCheckResult = StreamUtil.checkAndCreateKafkaTopic(s, initialData.force)
            streamCheckResult match {
              case Left(err) => errors += err
              case _ =>
            }
          } catch {
            case e: TopicExistsException =>
              val messages = ResourceBundle.getBundle("messages")
              errors += MessageFormat.format(
                messages.getString("rest.streams.create.kafka.cannot"),
                errors.mkString("\n")
              )
          }
        case s: ESSjStream =>
          val streamCheckResult = StreamUtil.checkAndCreateEsStream(s, initialData.force)
          streamCheckResult match {
            case Left(err) => errors += err
            case _ =>
          }
        case s: JDBCSjStream =>
          val streamCheckResult = StreamUtil.checkAndCreateJdbcStream(s, initialData.force)
          streamCheckResult match {
            case Left(err) => errors += err
            case _ =>
          }
      }
    }

    errors
  }
}
