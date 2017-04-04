package com.bwsw.sj.crud.rest.validator.instance

import com.bwsw.sj.common.DAL.model.{KafkaService, KafkaSjStream, TStreamService}
import com.bwsw.sj.common.rest.entities.module.{InstanceMetadata, RegularInstanceMetadata, SpecificationData}
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.SjStreamUtils._
import com.bwsw.sj.common.utils.StreamLiterals._
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ArrayBuffer

/**
  * Validator for Stream-processing regular module type
  *
  * @author Kseniya Tomskikh
  */
class RegularInstanceValidator extends InstanceValidator {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  override def validate(parameters: InstanceMetadata, specification: SpecificationData) = {
    logger.debug(s"Instance: ${parameters.name}. Start a validation of instance of regular-streaming type.")
    val errors = new ArrayBuffer[String]()
    errors ++= super.validateGeneralOptions(parameters)
    val regularInstanceMetadata = parameters.asInstanceOf[RegularInstanceMetadata]

    // 'checkpoint-mode' field
    Option(regularInstanceMetadata.checkpointMode) match {
      case None =>
        errors += createMessage("rest.validator.attribute.required", "checkpointMode")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("rest.validator.attribute.required", "checkpointMode")
        }
        else {
          if (!checkpointModes.contains(x)) {
            errors += createMessage("rest.validator.attribute.unknown.value", "checkpointMode", x) + ". " +
              createMessage("rest.validator.attribute.must.one_of", "checkpointMode", checkpointModes.mkString("[", ", ", "]"))
          }
        }
    }

    // 'checkpoint-interval' field
    if (regularInstanceMetadata.checkpointInterval <= 0) {
      errors += createMessage("rest.validator.attribute.required", "checkpointInterval") + ". " +
        createMessage("rest.validator.attribute.must.greater.than.zero", "checkpointInterval")
    }

    // 'event-wait-idle-time' field
    if (regularInstanceMetadata.eventWaitIdleTime <= 0) {
      errors += createMessage("rest.validator.attribute.must.greater.than.zero", "eventWaitIdleTime")
    }

    // 'state-management' field
    if (!stateManagementModes.contains(regularInstanceMetadata.stateManagement)) {
      errors += createMessage("rest.validator.attribute.unknown.value", "stateManagement", regularInstanceMetadata.stateManagement) + ". " +
        createMessage("rest.validator.attribute.must.one_of", "stateManagement", stateManagementModes.mkString("[", ", ", "]"))
    } else {
      if (regularInstanceMetadata.stateManagement != EngineLiterals.noneStateMode) {
        // 'state-full-checkpoint' field
        if (regularInstanceMetadata.stateFullCheckpoint <= 0) {
          errors += createMessage("rest.validator.attribute.must.greater.than.zero", "stateFullCheckpoint")
        }
      }
    }

    errors ++= validateStreamOptions(regularInstanceMetadata, specification)
  }

  private def validateStreamOptions(instance: RegularInstanceMetadata,
                                    specification: SpecificationData) = {
    logger.debug(s"Instance: ${instance.name}. Stream options validation.")
    val errors = new ArrayBuffer[String]()

    // 'inputs' field
    val inputModes = instance.inputs.map(i => getStreamMode(i))
    if (inputModes.exists(m => !streamModes.contains(m))) {
      errors += createMessage("rest.validator.unknown.stream.mode", streamModes.mkString("[", ", ", "]"))
    }
    val inputsCardinality = specification.inputs("cardinality").asInstanceOf[Array[Int]]
    if (instance.inputs.length < inputsCardinality(0)) {
      errors += createMessage("rest.validator.cardinality.cannot.less", "inputs", s"${inputsCardinality(0)}")
    }
    if (instance.inputs.length > inputsCardinality(1)) {
      errors += createMessage("rest.validator.cardinality.cannot.more", "inputs", s"${inputsCardinality(1)}")
    }

    val clearInputs = instance.inputs.map(clearStreamFromMode)
    if (doesContainDoubles(clearInputs)) {
      errors += createMessage("rest.validator.sources.not.unique", "Inputs")
    }
    val inputStreams = getStreams(clearInputs)
    clearInputs.foreach { streamName =>
      if (!inputStreams.exists(s => s.name == streamName)) {
        errors += createMessage("rest.validator.source_stream.not.exist", "Input", streamName)
      }
    }

    val inputTypes = specification.inputs("types").asInstanceOf[Array[String]]
    if (inputStreams.exists(s => !inputTypes.contains(s.streamType))) {
      errors += createMessage("rest.validator.source_stream.must.one.of", "Input", inputTypes.mkString("[", ", ", "]"))
    }

    val kafkaStreams = inputStreams.filter(s => s.streamType.equals(kafkaStreamType)).map(_.asInstanceOf[KafkaSjStream])
    if (kafkaStreams.nonEmpty) {
      if (kafkaStreams.exists(s => !s.service.isInstanceOf[KafkaService])) {
        errors += createMessage("rest.validator.service.must", "kafka streams", "KfkQ")
      }
    }

    // 'start-from' field
    val startFrom = instance.startFrom
    if (inputStreams.exists(s => s.streamType.equals(kafkaStreamType))) {
      if (!startFromModes.contains(startFrom)) {
        errors += createMessage("rest.validator.attribute.must.if.instance.have", "startFrom", startFromModes.mkString("[", ", ", "]"))
      }
    } else {
      if (!startFromModes.contains(startFrom)) {
        try {
          startFrom.toLong
        } catch {
          case ex: NumberFormatException =>
            errors += createMessage("rest.validator.attribute.must.one_of", "startFrom", s"${startFromModes.mkString("[", ", ", "]")} or timestamp")
        }
      }
    }

    // 'outputs' field
    val outputsCardinality = specification.outputs("cardinality").asInstanceOf[Array[Int]]
    if (instance.outputs.length < outputsCardinality(0)) {
      errors += createMessage("rest.validator.cardinality.cannot.less", "outputs", s"${outputsCardinality(0)}")
    }
    if (instance.outputs.length > outputsCardinality(1)) {
      errors += createMessage("rest.validator.cardinality.cannot.more", "outputs", s"${outputsCardinality(1)}")
    }
    if (doesContainDoubles(instance.outputs)) {
      errors += createMessage("rest.validator.sources.not.unique", "Outputs")
    }
    val outputStreams = getStreams(instance.outputs)
    instance.outputs.toList.foreach { streamName =>
      if (!outputStreams.exists(s => s.name == streamName)) {
        errors += createMessage("rest.validator.source_stream.not.exist", "Output", streamName)
      }
    }
    val outputTypes = specification.outputs("types").asInstanceOf[Array[String]]
    if (outputStreams.exists(s => !outputTypes.contains(s.streamType))) {
      errors += createMessage("rest.validator.source_stream.must.one.of", "Output", outputTypes.mkString("[", ", ", "]"))
    }

    // 'parallelism' field
    val partitions = getStreamsPartitions(inputStreams)
    val minPartitionCount = if (partitions.nonEmpty) partitions.min else 0
    errors ++= validateParallelism(instance.parallelism, minPartitionCount)

    val allStreams = inputStreams.union(outputStreams)
    val tStreamsServices = getStreamServices(allStreams.filter { s =>
      s.streamType.equals(tstreamType)
    })
    if (tStreamsServices.size != 1) {
      errors += createMessage("rest.validator.t_stream.same.service")
    } else {
      val service = serviceDAO.get(tStreamsServices.head)
      if (!service.get.isInstanceOf[TStreamService]) {
        errors += createMessage("rest.validator.service.must", "t-streams", "TstrQ")
      }
    }

    errors
  }
}
