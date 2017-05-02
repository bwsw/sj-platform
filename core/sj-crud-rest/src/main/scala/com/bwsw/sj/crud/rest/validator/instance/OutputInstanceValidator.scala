package com.bwsw.sj.crud.rest.validator.instance

import com.bwsw.sj.common.DAL.model.service.TStreamService
import com.bwsw.sj.common.DAL.model.stream.{SjStream, TStreamSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.entities.module.{InstanceMetadata, OutputInstanceMetadata, SpecificationData}
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.SjStreamUtils._
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ArrayBuffer

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
    * @param parameters    - input parameters for running module
    * @param specification - specification of module
    * @return - List of errors
    */
  override def validate(parameters: InstanceMetadata, specification: SpecificationData) = {
    logger.debug(s"Instance: ${parameters.name}. Start a validation of instance of output-streaming type.")
    val errors = new ArrayBuffer[String]()
    errors ++= super.validateGeneralOptions(parameters)
    val outputInstanceMetadata = parameters.asInstanceOf[OutputInstanceMetadata]

    Option(outputInstanceMetadata.checkpointMode) match {
      case None =>
        errors += createMessage("rest.validator.attribute.required", "checkpointMode")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("rest.validator.attribute.required", "checkpointMode")
        }
        else {
          if (!x.equals(EngineLiterals.everyNthMode)) {
            errors += createMessage("rest.validator.attribute.unknown.value", "checkpointMode", s"$x") + ". " +
              createMessage("rest.validator.attribute.not", "checkpointMode", EngineLiterals.everyNthMode)
          }
        }
    }

    // 'inputAvroSchema' field
    if (!outputInstanceMetadata.validateAvroSchema) {
      errors += createMessage("rest.validator.attribute.not", "inputAvroSchema", "Avro Schema")
    }

    // 'checkpoint-interval' field
    if (outputInstanceMetadata.checkpointInterval <= 0) {
      errors += createMessage("rest.validator.attribute.required", "checkpointInterval") + ". " +
        createMessage("rest.validator.attribute.must.greater.than.zero", "checkpointInterval")
    }

    errors ++= validateStreamOptions(outputInstanceMetadata, specification)
  }

  private def validateStreamOptions(instance: OutputInstanceMetadata,
                                    specification: SpecificationData) = {
    logger.debug(s"Instance: ${instance.name}. Stream options validation.")
    val errors = new ArrayBuffer[String]()

    // 'input' field
    var inputStream: Option[SjStream] = None
    Option(instance.input) match {
      case None =>
        errors += createMessage("rest.validator.attribute.required", "Input")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("rest.validator.attribute.required", "Input")
        }
        else {
          val inputMode: String = getStreamMode(x)
          if (!inputMode.equals(EngineLiterals.splitStreamMode)) {
            errors += createMessage("rest.validator.attribute.unknown.value", "stream-mode", EngineLiterals.splitStreamMode)
          }

          val inputStreamName = clearStreamFromMode(x)
          inputStream = getStream(inputStreamName)
          inputStream match {
            case None =>
              errors += createMessage("rest.validator.not.exist", s"Input stream '$inputStreamName'")
            case Some(stream) =>
              val inputTypes = specification.inputs("types").asInstanceOf[Array[String]]
              if (!inputTypes.contains(stream.streamType)) {
                errors += createMessage("rest.validator.attribute.must.one_of", "Input stream", inputTypes.mkString("[", ", ", "]"))
              } else {
                val input = stream.asInstanceOf[TStreamSjStream]
                val service = input.service
                if (!service.isInstanceOf[TStreamService]) {
                  errors += createMessage("rest.validator.service.must", "t-streams", "TstrQ")
                }

                errors ++= validateParallelism(instance.parallelism, input.partitions)
              }
          }
        }
    }

    // 'output' field
    Option(instance.output) match {
      case None =>
        errors += createMessage("rest.validator.attribute.required", "Output")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("rest.validator.attribute.required", "Output")
        }
        else {
          val outputStream = getStream(x)
          outputStream match {
            case None =>
              errors += createMessage("rest.validator.not.exist", s"Output stream '$x'")
            case Some(stream) =>
              val outputTypes = specification.outputs("types").asInstanceOf[Array[String]]
              if (!outputTypes.contains(stream.streamType)) {
                errors += createMessage("rest.validator.attribute.must.one_of", "Output stream", outputTypes.mkString("[", ", ", "]"))
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
          errors += createMessage("rest.validator.attribute.must.one_of", "startFrom", s"${startFromModes.mkString("[", ", ", "]")} or timestamp")
      }
    }

    errors
  }
}
