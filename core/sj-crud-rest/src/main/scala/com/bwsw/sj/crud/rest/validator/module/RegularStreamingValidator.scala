package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.common.DAL.model.{KafkaService, KafkaSjStream, TStreamSjStream, TStreamService}
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.utils.EngineConstants
import EngineConstants._
import com.bwsw.sj.common.utils.StreamConstants._
import com.bwsw.sj.common.rest.entities.module.{RegularInstanceMetadata, ModuleSpecification, InstanceMetadata}
import org.slf4j.{LoggerFactory, Logger}

import scala.collection.mutable.ArrayBuffer

/**
 * Validator for Stream-processing regular module type
 *
 * @author Kseniya Tomskikh
 */
class RegularStreamingValidator extends StreamingModuleValidator {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  /**
   * Validating input parameters for 'regular-streaming' module
   *
   * @param instanceParameters - input parameters for running module
   * @return - List of errors
   */
  override def validate(instanceParameters: InstanceMetadata, specification: ModuleSpecification) = {
    logger.debug(s"Instance: ${instanceParameters.name}. Start regular-streaming validation.")
    val parameters = instanceParameters.asInstanceOf[RegularInstanceMetadata]
    val generalErrors = super.validateGeneralOptions(parameters)
    val result = validateStreamOptions(parameters, specification, generalErrors)
    val errors = result._1

    // 'event-wait-time' field
    if (parameters.eventWaitTime <= 0) {
      errors += s"'Event-wait-time' attribute must be greater than zero"
    }

    // 'state-management' field
    if (!stateManagementModes.contains(parameters.stateManagement)) {
      errors += s"Unknown value of state-management attribute: ${parameters.stateManagement}. " +
        s"State-management must be one of: ${stateManagementModes.mkString("[", ",", "]")}"
    } else {
      if (parameters.stateManagement != "none") {
        // 'state-full-checkpoint' field
        if (parameters.stateFullCheckpoint <= 0) {
          errors += s"'State-full-checkpoint' attribute must be greater than zero"
        }
      }
    }

    (errors, result._2)
  }

  /**
   * Validating options of streams of instance for module
   *
   * @param instance - Input instance parameters
   * @param specification - Specification of module
   * @param errors - List of validating errors
   * @return - List of errors and validating instance (null, if errors non empty)
   */
  protected def validateStreamOptions(instance: InstanceMetadata,
                                      specification: ModuleSpecification,
                                      errors: ArrayBuffer[String]): (ArrayBuffer[String], Option[Instance]) = {
    logger.debug(s"Instance: ${instance.name}. Stream options validation.")
    val parameters = instance.asInstanceOf[RegularInstanceMetadata]

    // 'inputs' field
    val inputModes = parameters.inputs.map(i => getStreamMode(i))
    if (inputModes.exists(m => !streamModes.contains(m))) {
      errors += s"Unknown stream mode. Input streams must have on of mode: ${streamModes.mkString("[", ",", "]")}"
    }
    val inputsCardinality = specification.inputs("cardinality").asInstanceOf[Array[Int]]
    if (parameters.inputs.length < inputsCardinality(0)) {
      errors += s"Count of inputs cannot be less than ${inputsCardinality(0)}"
    }
    if (parameters.inputs.length > inputsCardinality(1)) {
      errors += s"Count of inputs cannot be more than ${inputsCardinality(1)}"
    }
    if (doesContainDoubles(parameters.inputs.toList)) {
      errors += s"Inputs is not unique"
    }
    val inputStreams = getStreams(parameters.inputs.toList.map(_.replaceAll("/split|/full", "")))
    parameters.inputs.toList.map(_.replaceAll("/split|/full", "")).foreach { streamName =>
      if (!inputStreams.exists(s => s.name == streamName)) {
        errors += s"Input stream '$streamName' does not exists"
      }
    }
    val inputTypes = specification.inputs("types").asInstanceOf[Array[String]]
    if (inputStreams.exists(s => !inputTypes.contains(s.streamType))) {
      errors += s"Input streams must be one of: ${inputTypes.mkString("[", ",", "]")}"
    }

    // 'start-from' field
    val startFrom = parameters.startFrom
    if (inputStreams.exists(s => s.streamType.equals(kafkaStreamType))) {
      if (!startFromModes.contains(startFrom)) {
        errors += s"'Start-from' attribute must be one of: ${startFromModes.mkString("[", ",", "]")}, if instance has the kafka-streams"
      }
    } else {
      try {
        startFrom.toLong
      } catch {
        case ex: NumberFormatException =>
          if (!startFromModes.contains(startFrom)) {
            errors += s"'Start-from' attribute is not one of: ${startFromModes.mkString("[", ",", "]")} or timestamp"
          }
      }
    }

    // 'outputs' field
    val outputsCardinality = specification.outputs("cardinality").asInstanceOf[Array[Int]]
    if (parameters.outputs.length < outputsCardinality(0)) {
      errors += s"Count of outputs cannot be less than ${outputsCardinality(0)}."
    }
    if (parameters.outputs.length > outputsCardinality(1)) {
      errors += s"Count of outputs cannot be more than ${outputsCardinality(1)}."
    }
    if (doesContainDoubles(parameters.outputs.toList)) {
      errors += s"Outputs is not unique"
    }
    val outputStreams = getStreams(parameters.outputs.toList)
    parameters.outputs.toList.foreach { streamName =>
      if (!outputStreams.exists(s => s.name == streamName)) {
        errors += s"Output stream '$streamName' does not exists"
      }
    }
    val outputTypes = specification.outputs("types").asInstanceOf[Array[String]]
    if (outputStreams.exists(s => !outputTypes.contains(s.streamType))) {
      errors += s"Output streams must be in: ${outputTypes.mkString(", ")}"
    }

    if (errors.isEmpty) {
      val allStreams = inputStreams.union(outputStreams)

      val tStreamsServices = getStreamServices(allStreams.filter { s =>
        s.streamType.equals(tStreamType)
      })
      if (tStreamsServices.size != 1) {
        errors += s"All t-streams should have the same service"
      } else {
        val service = serviceDAO.get(tStreamsServices.head)
        if (!service.get.isInstanceOf[TStreamService]) {
          errors += s"Service for t-streams must be 'TstrQ'"
        } else {
          checkTStreams(errors, allStreams.filter(s => s.streamType.equals(tStreamType)).map(_.asInstanceOf[TStreamSjStream]))
        }
      }

      val kafkaStreams = allStreams.filter(s => s.streamType.equals(kafkaStreamType)).map(_.asInstanceOf[KafkaSjStream])
      if (kafkaStreams.nonEmpty) {
        if (kafkaStreams.exists(s => !s.service.isInstanceOf[KafkaService])) {
          errors += s"Service for kafka streams must be 'KfkQ'"
        } else {
          checkKafkaStreams(errors, kafkaStreams)
        }
      }

      // 'parallelism' field
      val partitions = getStreamsPartitions(inputStreams)
      val minPartitionCount = if (partitions.nonEmpty) partitions.values.min else 0
      parameters.parallelism = checkParallelism(parameters.parallelism, minPartitionCount, errors)

      val validatedInstance = createInstance(parameters, partitions, allStreams.filter(s => s.streamType.equals(tStreamType)).toSet)
      (errors, validatedInstance)
    } else {
      (errors, None)
    }
  }

}
