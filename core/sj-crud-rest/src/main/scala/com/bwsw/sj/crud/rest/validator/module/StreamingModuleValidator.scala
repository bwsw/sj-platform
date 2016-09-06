package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.common.JsonSerializer
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.rest.entities.module.{InstanceMetadata, ModuleSpecification}
import com.bwsw.sj.common.utils.StreamConstants
import com.bwsw.sj.crud.rest.utils.{StreamUtil, ValidationUtils}
import kafka.common.TopicExistsException
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Trait of validator for modules
 *
 *
 * @author Kseniya Tomskikh
 */
abstract class StreamingModuleValidator extends ValidationUtils {
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  var serviceDAO: GenericMongoService[Service] = ConnectionRepository.getServiceManager
  var instanceDAO: GenericMongoService[Instance] = ConnectionRepository.getInstanceService
  val serializer: Serializer = new JsonSerializer

  /**
   * Validating input parameters for streaming module
   *
   * @param parameters - input parameters for running module
   * @return - List of errors
   */
  def validate(parameters: InstanceMetadata, specification: ModuleSpecification): (ArrayBuffer[String], Option[Instance])

  /**
   * Validation base instance options
   *
   * @param parameters - Instance parameters
   * @return - List of errors
   */
  protected def validateGeneralOptions(parameters: InstanceMetadata) = {
    logger.debug(s"Instance: ${parameters.name}. General options validation.")
    val errors = new ArrayBuffer[String]()

    // 'name' field
    Option(parameters.name) match {
      case None =>
        errors += s"'Name' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'Name' can not be empty"
        } else {
          if (!validateName(parameters.name)) {
            errors += s"Instance has incorrect name: ${parameters.name}. " +
              s"Name of instance must be contain digits, lowercase letters or hyphens. First symbol must be letter."
          }

          if (instanceDAO.get(parameters.name).isDefined) {
            errors += s"Instance with name: ${parameters.name} exists."
          }
        }
    }

    // 'checkpoint-interval' field
    if (parameters.checkpointInterval <= 0) {
      errors += s"'Checkpoint-interval' must be greater than zero."
    }

    // 'per-task-cores' field
    if (parameters.perTaskCores <= 0) {
      errors += s"'Per-task-cores' must be greater than zero."
    }

    // 'per-task-ram' field
    if (parameters.perTaskRam <= 0) {
      errors += s"'Per-task-ram' must be greater than zero."
    }

    // 'performance-reporting-interval' field
    if (parameters.performanceReportingInterval <= 0) {
      errors += "'Performance-reporting-interval' must be greater than zero."
    }

    // 'coordination-service' field
    Option(parameters.coordinationService) match {
      case None =>
        errors += s"'Coordination-service' is required"
      case Some(x) =>
        val coordService = serviceDAO.get(parameters.coordinationService)
        if (coordService.isDefined) {
          if (!coordService.get.isInstanceOf[ZKService]) {
            errors += s"'Coordination-service' ${parameters.coordinationService} is not ZKCoord."
          }
        } else {
          errors += s"'Coordination-service' ${parameters.coordinationService} is not exists."
        }
    }

    errors
  }

  protected def doesContainDoubles(list: List[String]): Boolean = {
    list.map(x => (x, 1)).groupBy(_._1).map(x => x._2.reduce { (a, b) => (a._1, a._2 + b._2) }).exists(x => x._2 > 1)
  }

  protected def getStreams(streamNames: List[String]): mutable.Buffer[SjStream] = {
    val streamsDAO = ConnectionRepository.getStreamService
    streamsDAO.getAll.filter(s => streamNames.contains(s.name))
  }

  /**
   * Getting service names for all streams (must be one element in list)
   *
   * @param streams All streams
   * @return List of service-names
   */
  def getStreamServices(streams: Seq[SjStream]): List[String] = {
    streams.map(s => (s.service.name, 1)).groupBy(_._1).keys.toList
  }

  /**
   * Checking and creating t-streams, if streams do not exist
   *
   * @param errors - List of all errors
   * @param allTStreams - all t-streams of instance
   */
  protected def checkTStreams(errors: ArrayBuffer[String], allTStreams: mutable.Buffer[TStreamSjStream]) = {
    logger.debug(s"Check t-streams.")
    allTStreams.foreach { (stream: TStreamSjStream) =>
      if (errors.isEmpty) {
        val streamCheckResult = StreamUtil.checkAndCreateTStream(stream)
        streamCheckResult match {
          case Left(err) => errors += err
          case _ =>
        }
      }
    }
  }

  /**
   * Checking and creating kafka topics, if topics do not exist
   *
   * @param errors - list of all errors
   * @param allKafkaStreams - all kafka streams of instance
   */
  def checkKafkaStreams(errors: ArrayBuffer[String], allKafkaStreams: mutable.Buffer[KafkaSjStream]) = {
    logger.debug(s"Check kafka streams.")
    allKafkaStreams.foreach { (stream: KafkaSjStream) =>
      if (errors.isEmpty) {
        try {
          val streamCheckResult = StreamUtil.checkAndCreateKafkaTopic(stream)
          streamCheckResult match {
            case Left(err) => errors += err
            case _ =>
          }
        } catch {
          case e: TopicExistsException =>
            logger.debug(s"Stream ${stream.name}. Kafka topic already exists.")
            errors += s"Cannot create kafka topic: ${e.getMessage}"
        }
      }
    }
  }

  protected def getStreamsPartitions(streams: Seq[SjStream]): Map[String, Int] = {
    Map(streams.map { stream =>
      stream.streamType match {
        case StreamConstants.`tStreamType` =>
          stream.name -> stream.asInstanceOf[TStreamSjStream].partitions
        case StreamConstants.`kafkaStreamType` =>
          stream.name -> stream.asInstanceOf[KafkaSjStream].partitions
      }

    }: _*)
  }

  /**
   * Validating 'parallelism' parameters of instance
   *
   * @param parallelism - Parallelism value
   * @param partitions - Min count of partitions of input streams
   * @param errors - List of errors
   * @return - Validated value of parallelism
   */
  protected def checkParallelism(parallelism: Any, partitions: Int, errors: ArrayBuffer[String]) = {
    parallelism match {
      case dig: Int =>
        if (dig <= 0) {
          errors += "Parallelism must be greater than zero"
        }
        if (dig > partitions) {
          errors += s"Parallelism ($dig) > minimum of partition count ($partitions) in all input streams."
        }
        dig
      case s: String =>
        if (!s.equals("max")) {
          errors += s"Parallelism must be int value or 'max'."
        }
        partitions
      case _ =>
        errors += "Unknown type of 'parallelism' parameter. Must be Int or String."
        null
    }
  }

  /**
   * Checking and creating elasticsearch streams, if it's not exists
   *
   * @param errors - list of all errors
   * @param allEsStreams - all elasticsearch streams of instance
   */
  def checkEsStreams(errors: ArrayBuffer[String], allEsStreams: List[ESSjStream]) = {
    logger.debug(s"Check elasticsearch streams.")
    allEsStreams.foreach { (stream: ESSjStream) =>
      if (errors.isEmpty) {
        val streamCheckResult = StreamUtil.checkAndCreateEsStream(stream)
        streamCheckResult match {
          case Left(err) => errors += err
          case _ =>
        }
      }
    }
  }

  /**
   * Checking and creating sql tables, if it's not exists
   *
   * @param errors - list of all errors
   * @param allJdbcStreams - all jdbc streams of instance
   */
  def checkJdbcStreams(errors: ArrayBuffer[String], allJdbcStreams: List[JDBCSjStream]) = {
    logger.debug(s"Check jdbc streams.")
    allJdbcStreams.foreach { (stream: JDBCSjStream) =>
      if (errors.isEmpty) {
        val streamCheckResult = StreamUtil.checkAndCreateJdbcStream(stream)
        streamCheckResult match {
          case Left(err) => errors += err
          case _ =>
        }
      }
    }
  }
}
