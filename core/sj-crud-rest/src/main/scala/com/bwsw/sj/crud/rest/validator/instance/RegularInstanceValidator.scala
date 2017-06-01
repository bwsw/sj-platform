package com.bwsw.sj.crud.rest.validator.instance

import com.bwsw.sj.common.dal.model.service.{KafkaServiceDomain, TStreamServiceDomain}
import com.bwsw.sj.common.dal.model.stream.KafkaStreamDomain
import com.bwsw.sj.common.si.model.instance.RegularInstance
import com.bwsw.sj.common.si.model.module.Specification
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.StreamLiterals._
import com.bwsw.sj.common.utils.StreamUtils._
import com.bwsw.sj.common.utils.{AvroRecordUtils, EngineLiterals}
import org.slf4j.{Logger, LoggerFactory}
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

/**
  * Validator for Stream-processing regular module type
  *
  * @author Kseniya Tomskikh
  */
class RegularInstanceValidator(implicit injector: Injector) extends InstanceValidator {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  override type T = RegularInstance

  override def validate(instance: T, specification: Specification): ArrayBuffer[String] = {
    logger.debug(s"Instance: ${instance.name}. Start a validation of instance of regular-streaming type.")
    val errors = new ArrayBuffer[String]()
    errors ++= super.validate(instance, specification
    )

    // 'checkpoint-mode' field
    Option(instance.checkpointMode) match {
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
    if (instance.checkpointInterval <= 0) {
      errors += createMessage("rest.validator.attribute.required", "checkpointInterval") + ". " +
        createMessage("rest.validator.attribute.must.greater.than.zero", "checkpointInterval")
    }

    // 'event-wait-idle-time' field
    if (instance.eventWaitIdleTime <= 0) {
      errors += createMessage("rest.validator.attribute.must.greater.than.zero", "eventWaitIdleTime")
    }

    // 'state-management' field
    if (!stateManagementModes.contains(instance.stateManagement)) {
      errors += createMessage("rest.validator.attribute.unknown.value", "stateManagement", instance.stateManagement) + ". " +
        createMessage("rest.validator.attribute.must.one_of", "stateManagement", stateManagementModes.mkString("[", ", ", "]"))
    } else {
      if (instance.stateManagement != EngineLiterals.noneStateMode) {
        // 'state-full-checkpoint' field
        if (instance.stateFullCheckpoint <= 0) {
          errors += createMessage("rest.validator.attribute.must.greater.than.zero", "stateFullCheckpoint")
        }
      }
    }

    if (Try(AvroRecordUtils.jsonToSchema(instance.inputAvroSchema)).isFailure)
      errors += createMessage("rest.validator.attribute.not", "inputAvroSchema", "Avro Schema")

    errors
  }

  override protected def validateStreamOptions(instance: RegularInstance, specification: Specification): Seq[String] = {
    logger.debug(s"Instance: ${instance.name}. Stream options validation.")
    val errors = new ArrayBuffer[String]()

    // 'inputs' field
    val inputModes = instance.inputs.map(i => getStreamMode(i))
    if (inputModes.exists(m => !streamModes.contains(m))) {
      errors += createMessage("rest.validator.unknown.stream.mode", streamModes.mkString("[", ", ", "]"))
    }
    val inputsCardinality = specification.inputs.cardinality
    if (instance.inputs.length < inputsCardinality(0)) {
      errors += createMessage("rest.validator.cardinality.cannot.less", "inputs", s"${inputsCardinality(0)}")
    }
    if (instance.inputs.length > inputsCardinality(1)) {
      errors += createMessage("rest.validator.cardinality.cannot.more", "inputs", s"${inputsCardinality(1)}")
    }

    val clearInputs = instance.inputs.map(clearStreamFromMode)
    if (doesContainDuplicates(clearInputs)) {
      errors += createMessage("rest.validator.sources.not.unique", "Inputs")
    }
    val inputStreams = getStreams(clearInputs)
    clearInputs.foreach { streamName =>
      if (!inputStreams.exists(s => s.name == streamName)) {
        errors += createMessage("rest.validator.source_stream.not.exist", "Input", streamName)
      }
    }

    val inputTypes = specification.inputs.types
    if (inputStreams.exists(s => !inputTypes.contains(s.streamType))) {
      errors += createMessage("rest.validator.source_stream.must.one.of", "Input", inputTypes.mkString("[", ", ", "]"))
    }

    val kafkaStreams = inputStreams.filter(s => s.streamType.equals(kafkaStreamType)).map(_.asInstanceOf[KafkaStreamDomain])
    if (kafkaStreams.nonEmpty) {
      if (kafkaStreams.exists(s => !s.service.isInstanceOf[KafkaServiceDomain])) {
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
        Try(startFrom.toLong) match {
          case Success(_) =>
          case Failure(_: NumberFormatException) =>
            errors += createMessage("rest.validator.attribute.must.one_of", "startFrom", s"${startFromModes.mkString("[", ", ", "]")} or timestamp")
          case Failure(e) => throw e
        }
      }
    }

    // 'outputs' field
    val outputsCardinality = specification.outputs.cardinality
    if (instance.outputs.length < outputsCardinality(0)) {
      errors += createMessage("rest.validator.cardinality.cannot.less", "outputs", s"${outputsCardinality(0)}")
    }
    if (instance.outputs.length > outputsCardinality(1)) {
      errors += createMessage("rest.validator.cardinality.cannot.more", "outputs", s"${outputsCardinality(1)}")
    }
    if (doesContainDuplicates(instance.outputs)) {
      errors += createMessage("rest.validator.sources.not.unique", "Outputs")
    }
    val outputStreams = getStreams(instance.outputs)
    instance.outputs.toList.foreach { streamName =>
      if (!outputStreams.exists(s => s.name == streamName)) {
        errors += createMessage("rest.validator.source_stream.not.exist", "Output", streamName)
      }
    }
    val outputTypes = specification.outputs.types
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
    if (tStreamsServices.nonEmpty) {
      if (tStreamsServices.size > 1) {
        errors += createMessage("rest.validator.t_stream.same.service")
      } else {
        val service = serviceRepository.get(tStreamsServices.head)
        if (!service.get.isInstanceOf[TStreamServiceDomain]) {
          errors += createMessage("rest.validator.service.must", "t-streams", "TstrQ")
        }
      }
    }

    errors
  }
}
