package com.bwsw.sj.crud.rest.validator.instance

import com.bwsw.sj.common.DAL.model.{TStreamService, KafkaService, KafkaSjStream}
import com.bwsw.sj.common.rest.entities.module.{InstanceMetadata, SpecificationData, WindowedInstanceMetadata}
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.SjStreamUtils._
import com.bwsw.sj.common.utils.StreamLiterals._
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ArrayBuffer

/**
  * Validator for Stream-processing-windowed module type
  *
  * @author Kseniya Tomskikh
  */
class WindowedInstanceValidator extends InstanceValidator {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  override def validate(parameters: InstanceMetadata, specification: SpecificationData) = {
    logger.debug(s"Instance: ${parameters.name}. Start windowed-streaming validation.")
    val errors = new ArrayBuffer[String]()
    errors ++= super.validateGeneralOptions(parameters)
    val windowedInstanceMetadata = parameters.asInstanceOf[WindowedInstanceMetadata]

    // 'state-management' field
    if (!stateManagementModes.contains(windowedInstanceMetadata.stateManagement)) {
      errors += createMessage("rest.validator.attribute.unknown.value", "state-management", windowedInstanceMetadata.stateManagement) + ". " +
        createMessage("rest.validator.attribute.must.one_of", "State-management", stateManagementModes.mkString("[", ", ", "]"))
    } else {
      if (windowedInstanceMetadata.stateManagement != EngineLiterals.noneStateMode) {
        // 'state-full-checkpoint' field
        if (windowedInstanceMetadata.stateFullCheckpoint <= 0) {
          errors += createMessage("rest.validator.attribute.must.greater.than.zero", "State-full-checkpoint")
        }
      }
    }

    // 'window' field
    if (windowedInstanceMetadata.window <= 0) {
      errors += createMessage("rest.validator.attribute.must.greater.than.zero", "Window")
    }

    // 'sliding-interval' field
    if (windowedInstanceMetadata.slidingInterval <= 0) {
      errors += createMessage("rest.validator.attribute.must.greater.than.zero", "Sliding-interval")
    }

    if (windowedInstanceMetadata.slidingInterval > windowedInstanceMetadata.window) {
      errors += createMessage("rest.validator.attribute.must.greater.or.equal", "Window", "sliding-interval")
    }

    Option(windowedInstanceMetadata.mainStream) match {
      case Some(x) =>
        errors ++= validateStreamOptions(windowedInstanceMetadata, specification)
      case None =>
        errors += createMessage("rest.validator.attribute.required", "Main-stream")
    }

    errors
  }

  def validateStreamOptions(instance: WindowedInstanceMetadata,
                            specification: SpecificationData): ArrayBuffer[String] = {
    logger.debug(s"Instance: ${instance.name}. Stream options validation.")
    val errors = new ArrayBuffer[String]()
    val inputs = instance.inputsOrEmptyList()

    // 'inputs' field
    val inputModes = inputs.map(i => getStreamMode(i))
    if (inputModes.exists(m => !streamModes.contains(m))) {
      errors += createMessage("rest.validator.unknown.stream.mode", streamModes.mkString("[", ", ", "]"))
    }
    val inputsCardinality = specification.inputs("cardinality").asInstanceOf[Array[Int]]
    if (inputs.length < inputsCardinality(0)) {
      errors += createMessage("rest.validator.cardinality.cannot.less", "inputs", s"${inputsCardinality(0)}")
    }
    if (inputs.length > inputsCardinality(1)) {
      errors += createMessage("rest.validator.cardinality.cannot.more", "inputs", s"${inputsCardinality(1)}")
    }

    val clearInputs = inputs.map(clearStreamFromMode)
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

    //'batch-fill-type' field
    Option(instance.batchFillType) match {
      case None =>
        errors += s"Batch-fill-type is required"
      case Some(batchFillType) =>
        //'type-name' field
        Option(batchFillType.typeName) match {
          case Some(x) =>
            if (!batchFillTypes.contains(x)) {
              errors += createMessage("rest.validator.attribute.unknown.value", "type-name' of 'batch-fill-type", x) + ". " +
                createMessage("rest.validator.attribute.must.one_of", "Type-name", batchFillTypes.mkString("[", ", ", "]"))
            } else {
              if (x == transactionIntervalMode && inputStreams.exists(x => x.streamType == kafkaStreamType)) {
                errors += createMessage(
                  "rest.validator.attribute.cannot.equal.if",
                  "Type-name' of 'batch-fill-type",
                  transactionIntervalMode,
                  kafkaStreamType
                )
              }
            }
          case None =>
            errors += createMessage("rest.validator.attribute.required", "Type-name' of 'batch-fill-type")
        }
        //'value' field
        Option(batchFillType.value) match {
          case Some(x) =>
            if (x <= 0) {
              errors += createMessage("rest.validator.attribute.must.greater.than.zero", "Value' of 'batch-fill-type")
            }
          case None =>
            errors += createMessage("rest.validator.attribute.required", "Value' of 'batch-fill-type")
        }

    }

    // 'start-from' field
    val startFrom = instance.startFrom
    if (inputStreams.exists(s => s.streamType.equals(kafkaStreamType))) {
      if (!startFromModes.contains(startFrom)) {
        errors += createMessage("rest.validator.attribute.must.if.instance.have", "Start-from", startFromModes.mkString("[", ", ", "]"))
      }
    } else {
      if (!startFromModes.contains(startFrom)) {
        try {
          startFrom.toLong
        } catch {
          case ex: NumberFormatException =>
            errors += createMessage("rest.validator.attribute.not.one.of", "Start-from", s"${startFromModes.mkString("[", ", ", "]")} or timestamp")
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
    val minPartitionCount = if (partitions.nonEmpty) partitions.values.min else 0
    errors ++= checkParallelism(instance.parallelism, minPartitionCount)

    val allStreams = inputStreams.union(outputStreams)
    val tStreamsServices = getStreamServices(allStreams.filter { s =>
      s.streamType.equals(tStreamType)
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
