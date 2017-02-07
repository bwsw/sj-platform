package com.bwsw.sj.crud.rest.validator.instance

import com.bwsw.common.JsonSerializer
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.rest.entities.module.{InstanceMetadata, SpecificationData}
import com.bwsw.sj.common.rest.utils.ValidationUtils
import com.bwsw.sj.common.utils.{EngineLiterals, MessageResourceUtils, StreamLiterals}
import com.bwsw.sj.crud.rest.utils.CompletionUtils
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Trait of validator for modules
 *
 *
 * @author Kseniya Tomskikh
 */
abstract class InstanceValidator extends ValidationUtils with CompletionUtils with MessageResourceUtils {
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
        errors += createMessage("rest.validator.attribute.required", "Name")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("rest.validator.attribute.required", "Name")
        }
        else {
          if (instanceDAO.get(x).isDefined) {
            errors += createMessage("rest.modules.instances.instance.exists", x)
          }

          if (!validateName(x)) {
            errors += createMessage("entity.error.incorrect.name", "Instance", x, "instance")
          }
        }
    }

    // 'per-task-cores' field
    if (parameters.perTaskCores <= 0) {
      errors += createMessage("rest.validator.attribute.must.greater.than.zero", "Per-task-cores")
    }

    // 'per-task-ram' field
    if (parameters.perTaskRam <= 0) {
      errors += createMessage("rest.validator.attribute.must.greater.than.zero", "Per-task-ram")
    }

    // 'performance-reporting-interval' field
    if (parameters.performanceReportingInterval <= 0) {
      errors += createMessage("rest.validator.attribute.must.greater.than.zero", "Performance-reporting-interval")
    }

    // 'coordination-service' field
    Option(parameters.coordinationService) match {
      case None =>
        errors += createMessage("rest.validator.attribute.required", "Coordination-service")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("rest.validator.attribute.required", "Coordination-service")
        }
        else {
          val coordService = serviceDAO.get(x)
          if (coordService.isDefined) {
            if (!coordService.get.isInstanceOf[ZKService]) {
              errors += createMessage("rest.validator.attribute.not", "Coordination-service", "ZKCoord")
            }
          } else {
            errors += createMessage("rest.validator.not.exist", s"'Coordination-service' $x")
          }
        }
    }

    errors
  }

  protected def doesContainDoubles(list: Array[String]): Boolean = {
    list.map(x => (x, 1)).groupBy(_._1).map(x => x._2.reduce { (a, b) => (a._1, a._2 + b._2) }).exists(x => x._2 > 1)
  }

  protected def getStreams(streamNames: Array[String]): mutable.Buffer[SjStream] = {
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

  protected def getStreamsPartitions(streams: Seq[SjStream]) = {
    streams.map { stream =>
      stream.streamType match {
        case StreamLiterals.`tstreamType` =>
          stream.asInstanceOf[TStreamSjStream].partitions
        case StreamLiterals.`kafkaStreamType` =>
          stream.asInstanceOf[KafkaSjStream].partitions
        case _ => 0
      }
    }
  }

  protected def validateParallelism(parallelism: Any, minimumNumberOfPartitions: Int) = {
    logger.debug(s"Validate a parallelism parameter.")
    val errors = new ArrayBuffer[String]()
    Option(parallelism) match {
      case None =>
        errors += createMessage("rest.validator.attribute.required", "Parallelism")
      case Some(x) =>
        x match {
          case dig: Int =>
            if (dig <= 0) {
              errors += createMessage("rest.validator.attribute.must.greater.than.zero", "Parallelism")
            }
            if (dig > minimumNumberOfPartitions) {
              errors += createMessage("rest.validator.attribute.must.greater.than.parallelism", s"$dig", s"$minimumNumberOfPartitions")
            }
          case s: String =>
            if (!s.equals("max")) {
              errors += createMessage("rest.validator.parameter.unknown.type", "parallelism", "digit or 'max'")
            }
          case _ =>
            errors += createMessage("rest.validator.parameter.unknown.type", "parallelism", "digit or 'max'")
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
