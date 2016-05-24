package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.model.{SjStream, JDBCService, ESService}
import com.bwsw.sj.common.ModuleConstants._
import com.bwsw.sj.common.StreamConstants._
import com.bwsw.sj.crud.rest.entities.module.{OutputInstanceMetadata, ModuleSpecification, InstanceMetadata}

import scala.collection.mutable.ArrayBuffer

/**
  * Created: 23/05/2016
  *
  * @author Kseniya Tomskikh
  */
class OutputStreamingValidator extends StreamingModuleValidator {

  override def streamOptionsValidate(parameters: InstanceMetadata, specification: ModuleSpecification, errors: ArrayBuffer[String]) = {
    var inputStream: SjStream = null
    if (parameters.inputs != null) {
      errors += s"Unknown attribute 'inputs'."
    }
    if (parameters.asInstanceOf[OutputInstanceMetadata].input != null) {
      val inputMode: String = getStreamMode(parameters.asInstanceOf[OutputInstanceMetadata].input)
      if (!inputMode.equals("split")) {
        errors += s"Unknown stream mode. Input stream must have modes 'split'."
      }

      inputStream = getStream(parameters.asInstanceOf[OutputInstanceMetadata].input.replaceAll("/split", ""))
      if (!inputStream.streamType.equals(tStream)) {
        errors += s"Input streams must be T-stream."
      }
    } else {

    }

    var outputStream: SjStream = null
    if (parameters.outputs != null) {
      errors += s"Unknown attribute 'outputs'."
    }
    if (parameters.asInstanceOf[OutputInstanceMetadata].output != null) {
      outputStream = getStream(parameters.asInstanceOf[OutputInstanceMetadata].output)
      val outputTypes = specification.outputs("types").asInstanceOf[Array[String]]
      if (!outputTypes.contains(outputStream.streamType)) {
        errors += s"Output streams must be in: ${outputTypes.mkString(", ")}."
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

      val esStreams = allStreams.filter(s => s.streamType.equals(esOutput))
      if (esStreams.nonEmpty) {
        if (esStreams.exists(s => !s.service.isInstanceOf[ESService])) {
          errors += s"Service for kafka-streams must be 'KfkQ'."
        } else {
          checkEsStreams(errors, esStreams.toList)
        }
      }

      val jdbcStreams = allStreams.filter(s => s.streamType.equals(jdbcOutput))
      if (jdbcStreams.nonEmpty) {
        if (jdbcStreams.exists(s => !s.service.isInstanceOf[JDBCService])) {
          errors += s"Service for kafka-streams must be 'KfkQ'."
        } else {
          checkJdbcStreams(errors, jdbcStreams.toList)
        }
      }

      parameters.parallelism = checkParallelism(parameters.parallelism, inputStream.partitions, errors)
      val partitions = getPartitionForStreams(Array(inputStream))

      parameters.inputs = Array(parameters.asInstanceOf[OutputInstanceMetadata].input)
      validatedInstance = createInstance(parameters, partitions, allStreams.filter(s => s.streamType.equals(tStream)).toSet)
    }
    (errors, validatedInstance)
  }

  override def validate(parameters: InstanceMetadata, specification: ModuleSpecification) = {
    val errors = super.generalOptionsValidate(parameters)
    streamOptionsValidate(parameters, specification, errors)
  }

}
