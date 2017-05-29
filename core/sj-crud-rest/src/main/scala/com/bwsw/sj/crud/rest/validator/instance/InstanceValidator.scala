package com.bwsw.sj.crud.rest.validator.instance

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.model.service.{ServiceDomain, ZKServiceDomain}
import com.bwsw.sj.common.dal.model.stream.{KafkaStreamDomain, StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.rest.utils.ValidationUtils._
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.si.model.module.Specification
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.{EngineLiterals, StreamLiterals}
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
abstract class InstanceValidator extends CompletionUtils {
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  var serviceRepository: GenericMongoRepository[ServiceDomain] = ConnectionRepository.getServiceRepository
  var instanceRepository: GenericMongoRepository[InstanceDomain] = ConnectionRepository.getInstanceRepository
  val serializer: JsonSerializer = new JsonSerializer

  /**
   * Validating input parameters for streaming module
   *
   * @param instance - input parameters for running module
   * @return - List of errors
   */
  def validate(instance: Instance, specification: Specification): ArrayBuffer[String]

  /**
   * Validation base instance options
   *
   * @param instance - Instance parameters
   * @return - List of errors
   */
  protected def validateGeneralOptions(instance: Instance) = {
    logger.debug(s"Instance: ${instance.name}. General options validation.")
    val errors = new ArrayBuffer[String]()

    // 'name' field
    Option(instance.name) match {
      case None =>
        errors += createMessage("rest.validator.attribute.required", "Name")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("rest.validator.attribute.required", "Name")
        }
        else {
          if (instanceRepository.get(x).isDefined) {
            errors += createMessage("rest.modules.instances.instance.exists", x)
          }

          if (!validateName(x)) {
            errors += createMessage("entity.error.incorrect.name", "Instance", x, "instance")
          }
        }
    }

    // 'per-task-cores' field
    if (instance.perTaskCores <= 0) {
      errors += createMessage("rest.validator.attribute.must.greater.than.zero", "perTaskCores")
    }

    // 'per-task-ram' field
    if (instance.perTaskRam <= 0) {
      errors += createMessage("rest.validator.attribute.must.greater.than.zero", "perTaskRam")
    }

    // 'performance-reporting-interval' field
    if (instance.performanceReportingInterval <= 0) {
      errors += createMessage("rest.validator.attribute.must.greater.than.zero", "performanceReportingInterval")
    }

    // 'coordination-service' field
    Option(instance.coordinationService) match {
      case None =>
        errors += createMessage("rest.validator.attribute.required", "coordinationService")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("rest.validator.attribute.required", "coordinationService")
        }
        else {
          val coordService = serviceRepository.get(x)
          if (coordService.isDefined) {
            if (!coordService.get.isInstanceOf[ZKServiceDomain]) {
              errors += createMessage("rest.validator.attribute.not", "coordinationService", "ZKCoord")
            }
          } else {
            errors += createMessage("rest.validator.not.exist", s"'coordinationService' $x")
          }
        }
    }

    errors
  }

  protected def doesContainDoubles(list: Array[String]): Boolean = {
    list.map(x => (x, 1)).groupBy(_._1).map(x => x._2.reduce { (a, b) => (a._1, a._2 + b._2) }).exists(x => x._2 > 1)
  }

  protected def getStreams(streamNames: Array[String]): mutable.Buffer[StreamDomain] = {
    val streamsDAO = ConnectionRepository.getStreamRepository
    streamsDAO.getAll.filter(s => streamNames.contains(s.name))
  }

  /**
   * Getting service names for all streams (must be one element in list)
   *
   * @param streams All streams
   * @return List of service-names
   */
  def getStreamServices(streams: Seq[StreamDomain]): List[String] = {
    streams.map(s => (s.service.name, 1)).groupBy(_._1).keys.toList
  }

  protected def getStreamsPartitions(streams: Seq[StreamDomain]) = {
    streams.map { stream =>
      stream.streamType match {
        case StreamLiterals.`tstreamType` =>
          stream.asInstanceOf[TStreamStreamDomain].partitions
        case StreamLiterals.`kafkaStreamType` =>
          stream.asInstanceOf[KafkaStreamDomain].partitions
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

  def getStreamMode(name: String): String = {
    val nameWithMode = name.split(s"/")
      if (nameWithMode.length == 1) EngineLiterals.splitStreamMode
      else nameWithMode(1)
  }
}
