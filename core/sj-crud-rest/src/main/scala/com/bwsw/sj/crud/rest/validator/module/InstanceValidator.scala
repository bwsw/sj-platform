package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.common.JsonSerializer
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.rest.entities.module.{InstanceMetadata, SpecificationData}
import com.bwsw.sj.common.rest.utils.ValidationUtils
import com.bwsw.sj.common.utils.{EngineLiterals, StreamLiterals}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Trait of validator for modules
 *
 *
 * @author Kseniya Tomskikh
 */
abstract class InstanceValidator extends ValidationUtils {
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
  def validate(parameters: InstanceMetadata, specification: SpecificationData): ArrayBuffer[String]

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
        if (instanceDAO.get(parameters.name).isDefined) {
          errors += s"Instance with name ${parameters.name} already exists"
        }

        if (!validateName(parameters.name)) {
          errors += s"Instance has incorrect name: ${parameters.name}. " +
            s"Name of instance must contain digits, lowercase letters or hyphens. First symbol must be a letter"
        }
    }

    // 'per-task-cores' field
    if (parameters.perTaskCores <= 0) {
      errors += s"'Per-task-cores' must be greater than zero"
    }

    // 'per-task-ram' field
    if (parameters.perTaskRam <= 0) {
      errors += s"'Per-task-ram' must be greater than zero"
    }

    // 'performance-reporting-interval' field
    if (parameters.performanceReportingInterval <= 0) {
      errors += "'Performance-reporting-interval' must be greater than zero"
    }

    // 'coordination-service' field
    Option(parameters.coordinationService) match {
      case None =>
        errors += s"'Coordination-service' is required"
      case Some(x) =>
        val coordService = serviceDAO.get(parameters.coordinationService)
        if (coordService.isDefined) {
          if (!coordService.get.isInstanceOf[ZKService]) {
            errors += s"'Coordination-service' ${parameters.coordinationService} is not ZKCoord"
          }
        } else {
          errors += s"'Coordination-service' ${parameters.coordinationService} does not exist"
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

  protected def getStreamsPartitions(streams: Seq[SjStream]): Map[String, Int] = {
    Map(streams.map { stream =>
      stream.streamType match {
        case StreamLiterals.`tStreamType` =>
          stream.name -> stream.asInstanceOf[TStreamSjStream].partitions
        case StreamLiterals.`kafkaStreamType` =>
          stream.name -> stream.asInstanceOf[KafkaSjStream].partitions
      }
    }: _*)
  }

  protected def checkParallelism(parallelism: Any, minimumNumberOfPartitions: Int) = {
    val errors = new ArrayBuffer[String]()
    Option(parallelism) match {
      case None =>
        errors += s"'Parallelism' is required"
      case Some(x) =>
        x match {
          case dig: Int =>
            if (dig <= 0) {
              errors += "'Parallelism' must be greater than zero"
            }
            if (dig > minimumNumberOfPartitions) {
              errors += s"'Parallelism' ($dig) is greater than minimum of partitions count ($minimumNumberOfPartitions) of input streams"
            }
          case s: String =>
            if (!s.equals("max")) {
              errors += "Unknown type of 'parallelism' parameter. Must be a digit or 'max'"
            }
          case _ =>
            errors += "Unknown type of 'parallelism' parameter. Must be a digit or 'max'"
        }
    }

    errors
  }

  def getStreamMode(name: String) = {
    if (name.contains(s"/${EngineLiterals.fullStreamMode}")) {
      EngineLiterals.fullStreamMode
    } else {
      EngineLiterals.splitStreamMode
    }
  }
}
