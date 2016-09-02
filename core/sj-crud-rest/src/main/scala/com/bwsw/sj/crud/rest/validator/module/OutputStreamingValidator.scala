package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.common.DAL.model.{TStreamSjStream, TStreamService, SjStream}
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineConstants._
import com.bwsw.sj.common.utils.StreamConstants._
import com.bwsw.sj.crud.rest.entities.module.{OutputInstanceMetadata, InstanceMetadata, ModuleSpecification}
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
    val errors = super.validateGeneralOptions(parameters)
    validateStreamOptions(parameters, specification, errors)
  }

  /**
   * Validating options of streams of instance for module
   *
   * @param parameters - Input instance parameters
   * @param specification - Specification of module
   * @param errors - List of validating errors
   * @return - List of errors and validating instance (null, if errors non empty)
   */
  override def validateStreamOptions(parameters: InstanceMetadata, specification: ModuleSpecification, errors: ArrayBuffer[String]) = {
    if (!parameters.checkpointMode.equals("every-nth")) {
      errors += s"Checkpoint-mode attribute for output-streaming module must be only 'every-nth'."
    }

    var inputStream: Option[SjStream] = None
    if (parameters.asInstanceOf[OutputInstanceMetadata].input != null) {
      val inputMode: String = getStreamMode(parameters.asInstanceOf[OutputInstanceMetadata].input)
      if (!inputMode.equals("split")) {
        errors += s"Unknown stream mode. Input stream must have modes 'split'."
      }

      val inputStreamName = parameters.asInstanceOf[OutputInstanceMetadata].input.replaceAll("/split", "")
      inputStream = getStream(inputStreamName)
      inputStream match {
        case None =>
          errors += s"Input stream '$inputStreamName' is not exists."
        case Some(stream) =>
          if (!stream.streamType.equals(tStreamType)) {
            errors += s"Input streams must be T-stream."
          }
      }
    } else {
      errors += s"Input stream attribute is empty."
    }

    var outputStream: Option[SjStream] = None
    if (parameters.outputs != null) {
      errors += s"Unknown attribute 'outputs'."
    }
    if (parameters.asInstanceOf[OutputInstanceMetadata].output != null) {
      outputStream = getStream(parameters.asInstanceOf[OutputInstanceMetadata].output)
      outputStream match {
        case None =>
          errors += s"Output stream '${parameters.asInstanceOf[OutputInstanceMetadata].output}' is not exists."
        case Some(stream) =>
          val outputTypes = specification.outputs("types").asInstanceOf[Array[String]]
          if (!outputTypes.contains(stream.streamType)) {
            errors += s"Output streams must be in: ${outputTypes.mkString(", ")}."
          }
      }
    } else {
      errors += s"Output stream attribute is empty."
    }

    val startFrom = parameters.asInstanceOf[OutputInstanceMetadata].startFrom
    if (!startFromModes.contains(startFrom)) {
      try {
        startFrom.toLong
      } catch {
        case ex: NumberFormatException =>
          errors += s"Start-from attribute is not 'oldest' or 'newest' or timestamp."
      }
    }

    var validatedInstance: Option[Instance] = None
    if (inputStream.isDefined && outputStream.isDefined) {
      val input = inputStream.get
      val allStreams = Array(input, outputStream.get)

      val service = allStreams.head.service
      if (!service.isInstanceOf[TStreamService]) {
        errors += s"Service for t-streams must be 'TstrQ'."
      } else {
        checkTStreams(errors, allStreams.filter(s => s.streamType.equals(tStreamType)).map(_.asInstanceOf[TStreamSjStream]).toBuffer)
      }

      parameters.parallelism = checkParallelism(parameters.parallelism, input.asInstanceOf[TStreamSjStream].partitions, errors)

      val partitions = getStreamsPartitions(Array(input))
      //parameters.inputs = Array(parameters.asInstanceOf[OutputInstanceMetadata].input) делается для того, чтобы функция по построению execution плана работала однотипно
      validatedInstance = createInstance(parameters, partitions, allStreams.filter(s => s.streamType.equals(tStreamType)).toSet)
    }
    (errors, validatedInstance)
  }
}
