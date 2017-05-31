package com.bwsw.sj.crud.rest.validator.instance

import com.bwsw.sj.common.dal.model.service.TStreamServiceDomain
import com.bwsw.sj.common.dal.model.stream.{StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.si.model.instance.{Instance, OutputInstance}
import com.bwsw.sj.common.si.model.module.Specification
import com.bwsw.sj.common.utils.{AvroRecordUtils, EngineLiterals}
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.StreamUtils._
import org.slf4j.{Logger, LoggerFactory}
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

/**
  *
  *
  * @author Kseniya Tomskikh
  */
class OutputInstanceValidator(implicit injector: Injector) extends InstanceValidator {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private val streamsDAO = connectionRepository.getStreamRepository

  private def getStream(streamName: String) = {
    streamsDAO.get(streamName)
  }

  /**
    * Validating input parameters for 'output-streaming' module
    *
    * @param instance      - input parameters for running module
    * @param specification - specification of module
    * @return - List of errors
    */
  override def validate(instance: Instance, specification: Specification): ArrayBuffer[String] = {
    logger.debug(s"Instance: ${instance.name}. Start a validation of instance of output-streaming type.")
    val errors = new ArrayBuffer[String]()
    errors ++= super.validateGeneralOptions(instance)
    val outputInstanceMetadata = instance.asInstanceOf[OutputInstance]

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

    // 'checkpoint-interval' field
    if (outputInstanceMetadata.checkpointInterval <= 0) {
      errors += createMessage("rest.validator.attribute.required", "checkpointInterval") + ". " +
        createMessage("rest.validator.attribute.must.greater.than.zero", "checkpointInterval")
    }

    if (Try(AvroRecordUtils.jsonToSchema(outputInstanceMetadata.inputAvroSchema)).isFailure)
      errors += createMessage("rest.validator.attribute.not", "inputAvroSchema", "Avro Schema")

    errors ++= validateStreamOptions(outputInstanceMetadata, specification)
  }

  private def validateStreamOptions(instance: OutputInstance, specification: Specification) = {
    logger.debug(s"Instance: ${instance.name}. Stream options validation.")
    val errors = new ArrayBuffer[String]()

    // 'input' field
    var inputStream: Option[StreamDomain] = None
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
              val inputTypes = specification.inputs.types
              if (!inputTypes.contains(stream.streamType)) {
                errors += createMessage("rest.validator.attribute.must.one_of", "Input stream", inputTypes.mkString("[", ", ", "]"))
              } else {
                val input = stream.asInstanceOf[TStreamStreamDomain]
                val service = input.service
                if (!service.isInstanceOf[TStreamServiceDomain]) {
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
              val outputTypes = specification.outputs.types
              if (!outputTypes.contains(stream.streamType)) {
                errors += createMessage("rest.validator.attribute.must.one_of", "Output stream", outputTypes.mkString("[", ", ", "]"))
              }
          }
        }
    }

    // 'start-from' field
    val startFrom = instance.startFrom
    if (!startFromModes.contains(startFrom)) {
      Try(startFrom.toLong) match {
        case Success(_) =>
        case Failure(_: NumberFormatException) =>
          errors += createMessage("rest.validator.attribute.must.one_of", "startFrom", s"${startFromModes.mkString("[", ", ", "]")} or timestamp")
        case Failure(e) => throw e
      }
    }

    errors
  }
}
