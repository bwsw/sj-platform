package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.model.{SjStream, TStreamService, TStreamSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.entities.module.{InstanceMetadata, ModuleSpecification, OutputInstanceMetadata}
import com.bwsw.sj.common.utils.EngineConstants._
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ArrayBuffer

/**
 *
 *
 * @author Kseniya Tomskikh
 */
class OutputStreamingValidator extends StreamingModuleValidator {

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
  override def validate(parameters: InstanceMetadata, specification: ModuleSpecification) = {
    logger.debug(s"Instance: ${parameters.name}. Start output-streaming validation.")
    val generalErrors = super.validateGeneralOptions(parameters)
    val outputInstanceMetadata = parameters.asInstanceOf[OutputInstanceMetadata]
    val result = validateStreamOptions(outputInstanceMetadata, specification, generalErrors)
    val errors = result._1

    if (!parameters.checkpointMode.equals("every-nth")) {
      errors += s"'Checkpoint-mode' attribute for output-streaming module must be only 'every-nth'"
    }

    (errors, result._2)
  }

  /**
   * Validating options of streams of instance for module
   *
   * @param parameters - Input instance parameters
   * @param specification - Specification of module
   * @param errors - List of validating errors
   * @return - List of errors and validating instance (null, if errors non empty)
   */
  def validateStreamOptions(parameters: OutputInstanceMetadata, specification: ModuleSpecification, errors: ArrayBuffer[String]) = {

    // 'inputs' field
    var inputStream: Option[SjStream] = None
    if (parameters.input != null) {
      val inputMode: String = getStreamMode(parameters.input)
      if (!inputMode.equals("split")) {
        errors += s"Unknown stream mode. Input stream must have the mode 'split'"
      }

      val inputStreamName = parameters.input.replaceAll("/split", "")
      inputStream = getStream(inputStreamName)
      inputStream match {
        case None =>
          errors += s"Input stream '$inputStreamName' does not exists"
        case Some(stream) =>
          val inputTypes = specification.inputs("types").asInstanceOf[Array[String]]
          if (!inputTypes.contains(stream.streamType)) {
            errors += s"Input stream must be one of: ${inputTypes.mkString("[", ",", "]")}"
          }
      }
    } else {
      errors += s"'Input' attribute is required"
    }

    if (parameters.output != null) {
      val outputStream = getStream(parameters.output)
      outputStream match {
        case None =>
          errors += s"Output stream '${parameters.output}' does not exists"
        case Some(stream) =>
          val outputTypes = specification.outputs("types").asInstanceOf[Array[String]]
          if (!outputTypes.contains(stream.streamType)) {
            errors += s"Output streams must be one of: ${outputTypes.mkString("[", ",", "]")}."
          }
      }
    } else {
      errors += s"'Output' attribute is required"
    }

    // 'start-from' field
    val startFrom = parameters.startFrom
    if (!startFromModes.contains(startFrom)) {
      try {
        startFrom.toLong
      } catch {
        case ex: NumberFormatException =>
          errors += s"'Start-from' attribute is not one of: ${startFromModes.mkString("[", ",", "]")} or timestamp"
      }
    }

    var validatedInstance: Option[Instance] = None
    if (errors.isEmpty) {
      val input = inputStream.get.asInstanceOf[TStreamSjStream]

      val service = input.service
      if (!service.isInstanceOf[TStreamService]) {
        errors += s"Service for t-streams must be 'TstrQ'"
      } else {
        checkTStreams(errors, ArrayBuffer(input))
      }

      parameters.parallelism = checkParallelism(parameters.parallelism, input.partitions, errors)

      val partitions = getStreamsPartitions(Array(input))
      validatedInstance = createInstance(parameters, partitions, Set(input))
    }

    (errors, validatedInstance)
  }
}
