package com.bwsw.sj.crud.rest.validator.instance

import com.bwsw.sj.common.DAL.model.{SjStream, TStreamService, TStreamSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.entities.module.{InstanceMetadata, OutputInstanceMetadata, SpecificationData}
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.EngineLiterals._
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ArrayBuffer
import com.bwsw.sj.common.utils.SjStreamUtils._

/**
 *
 *
 * @author Kseniya Tomskikh
 */
class OutputInstanceValidator extends InstanceValidator {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private val streamsDAO = ConnectionRepository.getStreamService

  private def getStream(streamName: String) = {
    streamsDAO.get(streamName)
  }

  /**
   * Validating input parameters for 'output-streaming' module
   *
   * @param parameters - input parameters for running module
   * @param specification - specification of module
   * @return - List of errors
   */
  override def validate(parameters: InstanceMetadata, specification: SpecificationData) = {
    logger.debug(s"Instance: ${parameters.name}. Start output-streaming validation.")
    val errors = new ArrayBuffer[String]()
    errors ++= super.validateGeneralOptions(parameters)
    val outputInstanceMetadata = parameters.asInstanceOf[OutputInstanceMetadata]

    Option(outputInstanceMetadata.checkpointMode) match {
      case None =>
        errors += s"'Checkpoint-mode' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'Checkpoint-mode' is required"
        }
        else {
          if (!x.equals(EngineLiterals.everyNthMode)) {
            errors += s"Unknown value of 'checkpoint-mode' attribute: '$x'. " +
              s"'Checkpoint-mode' attribute for output-streaming module must be only '${EngineLiterals.everyNthMode}'"
          }
        }
    }

    // 'checkpoint-interval' field
    if (outputInstanceMetadata.checkpointInterval <= 0) {
      errors += s"'Checkpoint-interval' must be greater than zero"
    }

    errors ++= validateStreamOptions(outputInstanceMetadata, specification)
  }

  private def validateStreamOptions(instance: OutputInstanceMetadata,
                                    specification: SpecificationData) = {
    logger.debug(s"Instance: ${instance.name}. Stream options validation.")
    val errors = new ArrayBuffer[String]()

    // 'inputs' field
    var inputStream: Option[SjStream] = None
    Option(instance.input) match {
      case None =>
        errors += s"'Input' attribute is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'Input' attribute is required"
        }
        else {
          val inputMode: String = getStreamMode(x)
          if (!inputMode.equals(EngineLiterals.splitStreamMode)) {
            errors += s"Unknown value of 'stream-mode' attribute. Input stream must have the mode '${EngineLiterals.splitStreamMode}'"
          }

          val inputStreamName = clearStreamFromMode(x)
          inputStream = getStream(inputStreamName)
          inputStream match {
            case None =>
              errors += s"Input stream '$inputStreamName' does not exist"
            case Some(stream) =>
              val inputTypes = specification.inputs("types").asInstanceOf[Array[String]]
              if (!inputTypes.contains(stream.streamType)) {
                errors += s"Input stream must be one of: ${inputTypes.mkString("[", ", ", "]")}"
              }
          }
        }
    }

    Option(instance.output) match {
      case None =>
        errors += "'Output' attribute is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += "'Output' attribute is required"
        }
        else {
          val outputStream = getStream(x)
          outputStream match {
            case None =>
              errors += s"Output stream '$x' does not exist"
            case Some(stream) =>
              val outputTypes = specification.outputs("types").asInstanceOf[Array[String]]
              if (!outputTypes.contains(stream.streamType)) {
                errors += s"Output streams must be one of: ${outputTypes.mkString("[", ", ", "]")}"
              }
          }
        }
    }

    // 'start-from' field
    val startFrom = instance.startFrom
    if (!startFromModes.contains(startFrom)) {
      try {
        startFrom.toLong
      } catch {
        case ex: NumberFormatException =>
          errors += s"'Start-from' attribute is not one of: ${startFromModes.mkString("[", ", ", "]")} or timestamp"
      }
    }

    val input = inputStream.get.asInstanceOf[TStreamSjStream]
    val service = input.service
    if (!service.isInstanceOf[TStreamService]) {
      errors += s"Service for t-streams must be 'TstrQ'"
    }

    errors ++= checkParallelism(instance.parallelism, input.partitions)

    errors
  }
}
