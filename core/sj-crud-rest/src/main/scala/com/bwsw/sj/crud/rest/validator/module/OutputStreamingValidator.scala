package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.model.{SjStream, TStreamService, TStreamSjStream}
import com.bwsw.sj.common.ModuleConstants._
import com.bwsw.sj.common.StreamConstants._
import com.bwsw.sj.crud.rest.entities.module.{InstanceMetadata, ModuleSpecification, OutputInstanceMetadata}
import org.slf4j.{LoggerFactory, Logger}

import scala.collection.mutable.ArrayBuffer

/**
  * Created: 23/05/2016
  *
  * @author Kseniya Tomskikh
  */
class OutputStreamingValidator extends StreamingModuleValidator {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  /**
    * Validating options of streams of instance for module
    *
    * @param parameters - Input instance parameters
    * @param specification - Specification of module
    * @param errors - List of validating errors
    * @return - List of errors and validating instance (null, if errors non empty)
    */
  override def streamOptionsValidate(parameters: InstanceMetadata, specification: ModuleSpecification, errors: ArrayBuffer[String]) = {
    if (!parameters.checkpointMode.equals("every-nth")) {
      errors += s"Checkpoint-mode attribute for output-streaming module must be only 'every-nth'."
    }

    var inputStream: SjStream = null
    if (parameters.inputs != null) {
      errors += s"Unknown attribute 'inputs'."
    }
    if (parameters.asInstanceOf[OutputInstanceMetadata].input != null) {
      val inputMode: String = getStreamMode(parameters.asInstanceOf[OutputInstanceMetadata].input)
      if (!inputMode.equals("split")) {
        errors += s"Unknown stream mode. Input stream must have modes 'split'."
      }

      val inputStreamName = parameters.asInstanceOf[OutputInstanceMetadata].input.replaceAll("/split", "")
      inputStream = getStream(inputStreamName)
      if (inputStream == null) {
        errors += s"Input stream '$inputStreamName' is not exists."
      } else {
        if (!inputStream.streamType.equals(tStreamType)) {
          errors += s"Input streams must be T-stream."
        }
      }
    } else {
      errors += s"Input stream attribute is empty."
    }

    var outputStream: SjStream = null
    if (parameters.outputs != null) {
      errors += s"Unknown attribute 'outputs'."
    }
    if (parameters.asInstanceOf[OutputInstanceMetadata].output != null) {
      outputStream = getStream(parameters.asInstanceOf[OutputInstanceMetadata].output)
      if (outputStream == null) {
        errors += s"Output stream '${parameters.asInstanceOf[OutputInstanceMetadata].output}' is not exists."
      } else {
        val outputTypes = specification.outputs("types").asInstanceOf[Array[String]]
        if (!outputTypes.contains(outputStream.streamType)) {
          errors += s"Output streams must be in: ${outputTypes.mkString(", ")}."
        }
      }
    } else {
      errors += s"Output stream attribute is empty."
    }

    val startFrom = parameters.startFrom
    if (!startFromModes.contains(startFrom)) {
      try {
        startFrom.toLong
      } catch {
        case ex: NumberFormatException =>
          errors += s"Start-from attribute is not 'oldest' or 'newest' or timestamp."
      }
    }

    var validatedInstance: Instance = null
    if (inputStream != null && outputStream != null) {
      val allStreams = Array(inputStream, outputStream)

      val service = allStreams.head.service
      if (!service.isInstanceOf[TStreamService]) {
        errors += s"Service for t-streams must be 'TstrQ'."
      } else {
        checkTStreams(errors, allStreams.filter(s => s.streamType.equals(tStreamType)).map(_.asInstanceOf[TStreamSjStream]).toBuffer)
      }

      parameters.parallelism = checkParallelism(parameters.parallelism, inputStream.asInstanceOf[TStreamSjStream].partitions, errors)

      val partitions = getPartitionForStreams(Array(inputStream))
      parameters.inputs = Array(parameters.asInstanceOf[OutputInstanceMetadata].input)
      validatedInstance = createInstance(parameters, partitions, allStreams.filter(s => s.streamType.equals(tStreamType)).toSet)
    }
    (errors, validatedInstance)
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
    val errors = super.generalOptionsValidate(parameters)
    streamOptionsValidate(parameters, specification, errors)
  }

}
