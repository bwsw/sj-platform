package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.common.DAL.model.{KafkaService, KafkaSjStream, TStreamSjStream, TStreamService}
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.utils.EngineConstants
import EngineConstants._
import com.bwsw.sj.common.utils.StreamConstants._
import com.bwsw.sj.crud.rest.entities.module.{RegularInstanceMetadata, ModuleSpecification, InstanceMetadata}
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
    val result = super.validate(instanceParameters, specification)
    val errors = result._1

    if (!stateManagementModes.contains(parameters.stateManagement)) {
      errors += s"Unknown value of state-management attribute: ${parameters.stateManagement}. " +
        s"State-management must be 'none' or 'ram' or 'rocks'."
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
  override protected def validateStreamOptions(instance: InstanceMetadata,
                                               specification: ModuleSpecification,
                                               errors: ArrayBuffer[String]): (ArrayBuffer[String], Option[Instance]) = {
    logger.debug(s"Instance: ${instance.name}. Stream options validation.")
    val parameters = instance.asInstanceOf[RegularInstanceMetadata]

    val inputModes = parameters.inputs.map(i => getStreamMode(i))
    if (inputModes.exists(m => !streamModes.contains(m))) {
      errors += s"Unknown stream modes. Input streams must have modes 'split' or 'full'."
    }
    val inputsCardinality = specification.inputs("cardinality").asInstanceOf[Array[Int]]
    if (parameters.inputs.length < inputsCardinality(0)) {
      errors += s"Count of inputs cannot be less than ${inputsCardinality(0)}."
    }
    if (parameters.inputs.length > inputsCardinality(1)) {
      errors += s"Count of inputs cannot be more than ${inputsCardinality(1)}."
    }
    if (doesContainDoubles(parameters.inputs.toList)) {
      errors += s"Inputs is not unique."
    }
    val inputStreams = getStreams(parameters.inputs.toList.map(_.replaceAll("/split|/full", "")))
    parameters.inputs.toList.map(_.replaceAll("/split|/full", "")).foreach { streamName =>
      if (!inputStreams.exists(s => s.name == streamName)) {
        errors += s"Input stream '$streamName' is not exists."
      }
    }

    val inputTypes = specification.inputs("types").asInstanceOf[Array[String]]
    if (inputStreams.exists(s => !inputTypes.contains(s.streamType))) {
      errors += s"Input streams must be in: ${inputTypes.mkString(", ")}.."
    }

    val outputsCardinality = specification.outputs("cardinality").asInstanceOf[Array[Int]]
    if (parameters.outputs.length < outputsCardinality(0)) {
      errors += s"Count of outputs cannot be less than ${outputsCardinality(0)}."
    }
    if (parameters.outputs.length > outputsCardinality(1)) {
      errors += s"Count of outputs cannot be more than ${outputsCardinality(1)}."
    }
    if (doesContainDoubles(parameters.outputs.toList)) {
      errors += s"Outputs is not unique."
    }
    val outputStreams = getStreams(parameters.outputs.toList)
    parameters.outputs.toList.foreach { streamName =>
      if (!outputStreams.exists(s => s.name == streamName)) {
        errors += s"Output stream '$streamName' is not exists."
      }
    }
    val outputTypes = specification.outputs("types").asInstanceOf[Array[String]]
    if (outputStreams.exists(s => !outputTypes.contains(s.streamType))) {
      errors += s"Output streams must be in: ${outputTypes.mkString(", ")}."
    }

    if (errors.isEmpty) {
      val allStreams = inputStreams.union(outputStreams)

      val startFrom = parameters.startFrom
      if (!startFromModes.contains(startFrom)) {
        if (allStreams.exists(s => s.streamType.equals(kafkaStreamType))) {
          errors += s"Start-from attribute must be 'oldest' or 'newest', if instance have kafka-streams."
        } else {
          try {
            startFrom.toLong
          } catch {
            case ex: NumberFormatException =>
              errors += s"Start-from attribute is not 'oldest' or 'newest' or timestamp."
          }
        }
      }

      val tStreamsServices = getStreamServices(allStreams.filter { s =>
        s.streamType.equals(tStreamType)
      })
      if (tStreamsServices.size != 1) {
        errors += s"All t-streams should have the same service."
      } else {
        val service = serviceDAO.get(tStreamsServices.head)
        if (!service.get.isInstanceOf[TStreamService]) {
          errors += s"Service for t-streams must be 'TstrQ'."
        } else {
          checkTStreams(errors, allStreams.filter(s => s.streamType.equals(tStreamType)).map(_.asInstanceOf[TStreamSjStream]))
        }
      }

      val kafkaStreams = allStreams.filter(s => s.streamType.equals(kafkaStreamType)).map(_.asInstanceOf[KafkaSjStream])
      if (kafkaStreams.nonEmpty) {
        if (kafkaStreams.exists(s => !s.service.isInstanceOf[KafkaService])) {
          errors += s"Service for kafka-streams must be 'KfkQ'."
        } else {
          checkKafkaStreams(errors, kafkaStreams)
        }
      }

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
