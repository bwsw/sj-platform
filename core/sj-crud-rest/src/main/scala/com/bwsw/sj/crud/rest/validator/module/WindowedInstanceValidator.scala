package com.bwsw.sj.crud.rest.validator.module

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
      errors += s"Unknown value of state-management attribute: ${windowedInstanceMetadata.stateManagement}. " +
        s"State-management must be one of: ${stateManagementModes.mkString("[", ", ", "]")}"
    } else {
      if (windowedInstanceMetadata.stateManagement != EngineLiterals.noneStateMode) {
        // 'state-full-checkpoint' field
        if (windowedInstanceMetadata.stateFullCheckpoint <= 0) {
          errors += s"'State-full-checkpoint' attribute must be greater than zero"
        }
      }
    }

    // 'window' field
    if (windowedInstanceMetadata.window <= 0) {
      errors += s"'Window' must be greater than zero"
    }

    // 'sliding-interval' field
    if (windowedInstanceMetadata.slidingInterval <= 0) {
      errors += s"'Sliding-interval' must be greater than zero"
    }

    if (windowedInstanceMetadata.slidingInterval > windowedInstanceMetadata.window) {
      errors += s"'Window' must be greater or equal than 'sliding-interval'"
    }

    Option(windowedInstanceMetadata.mainStream) match {
      case Some(x) =>
        errors ++= validateStreamOptions(windowedInstanceMetadata, specification)
      case None =>
        errors += s"'Main-stream' is required"

    }

    errors
  }

  def validateStreamOptions(instance: WindowedInstanceMetadata,
                            specification: SpecificationData): ArrayBuffer[String] = {
    logger.debug(s"Instance: ${instance.name}. Stream options validation.")
    val errors = new ArrayBuffer[String]()
    val inputs = instance.getInputs()

    // 'inputs' field
    val inputModes = inputs.map(i => getStreamMode(i))
    if (inputModes.exists(m => !streamModes.contains(m))) {
      errors += s"Unknown stream mode. Input streams must have one of mode: ${streamModes.mkString("[", ", ", "]")}"
    }
    val inputsCardinality = specification.inputs("cardinality").asInstanceOf[Array[Int]]
    if (inputs.length < inputsCardinality(0)) {
      errors += s"Count of inputs cannot be less than ${inputsCardinality(0)}"
    }
    if (inputs.length > inputsCardinality(1)) {
      errors += s"Count of inputs cannot be more than ${inputsCardinality(1)}"
    }

    val clearInputs = inputs.map(clearStreamFromMode)
    if (doesContainDoubles(clearInputs)) {
      errors += s"Inputs is not unique"
    }
    val inputStreams = getStreams(clearInputs)
    clearInputs.foreach { streamName =>
      if (!inputStreams.exists(s => s.name == streamName)) {
        errors += s"Input stream '$streamName' does not exist"
      }
    }

    val inputTypes = specification.inputs("types").asInstanceOf[Array[String]]
    if (inputStreams.exists(s => !inputTypes.contains(s.streamType))) {
      errors += s"Input streams must be one of: ${inputTypes.mkString("[", ", ", "]")}"
    }

    val kafkaStreams = inputStreams.filter(s => s.streamType.equals(kafkaStreamType)).map(_.asInstanceOf[KafkaSjStream])
    if (kafkaStreams.nonEmpty) {
      if (kafkaStreams.exists(s => !s.service.isInstanceOf[KafkaService])) {
        errors += s"Service for kafka streams must be 'KfkQ'"
      }
    }

    //'batch-fill-type' field
    Option(instance.batchFillType) match {
      case None =>
        errors += s"'Batch-fill-type' is required"
      case Some(batchFillType) =>
        //'type-name' field
        Option(batchFillType.typeName) match {
          case Some(x) =>
            if (!batchFillTypes.contains(x)) {
              errors += s"Unknown value of 'type-name' of 'batch-fill-type' attribute: '$x'. " +
                s"'Type-name' must be one of: ${batchFillTypes.mkString("[", ", ", "]")}"
            } else {
              if (x == transactionIntervalMode && inputStreams.exists(x => x.streamType == kafkaStreamType)) {
                errors += s"'Type-name' of 'batch-fill-type' cannot be equal '$transactionIntervalMode', " +
                  s"if there are the '$kafkaStreamType' type inputs"
              }
            }
          case None =>
            errors += s"'Type-name' of 'batch-fill-type' is required"
        }
        //'value' field
        Option(batchFillType.value) match {
          case Some(x) =>
            if (x <= 0) {
              errors += s"'Value' of 'batch-fill-type' must be greater than zero"
            }
          case None =>
            errors += s"'Value' of 'batch-fill-type' is required"
        }

    }

    // 'start-from' field
    val startFrom = instance.startFrom
    if (inputStreams.exists(s => s.streamType.equals(kafkaStreamType))) {
      if (!startFromModes.contains(startFrom)) {
        errors += s"'Start-from' attribute must be one of: ${startFromModes.mkString("[", ", ", "]")}, " +
          s"if instance inputs have the '$kafkaStreamType' type streams"
      }
    } else {
      if (!startFromModes.contains(startFrom)) {
        try {
          startFrom.toLong
        } catch {
          case ex: NumberFormatException =>
            errors += s"'Start-from' attribute is not one of: ${startFromModes.mkString("[", ", ", "]")} or timestamp"
        }
      }
    }

    // 'outputs' field
    val outputsCardinality = specification.outputs("cardinality").asInstanceOf[Array[Int]]
    if (instance.outputs.length < outputsCardinality(0)) {
      errors += s"Count of outputs cannot be less than ${outputsCardinality(0)}."
    }
    if (instance.outputs.length > outputsCardinality(1)) {
      errors += s"Count of outputs cannot be more than ${outputsCardinality(1)}."
    }
    if (doesContainDoubles(instance.outputs)) {
      errors += s"Outputs is not unique"
    }
    val outputStreams = getStreams(instance.outputs)
    instance.outputs.toList.foreach { streamName =>
      if (!outputStreams.exists(s => s.name == streamName)) {
        errors += s"Output stream '$streamName' does not exist"
      }
    }
    val outputTypes = specification.outputs("types").asInstanceOf[Array[String]]
    if (outputStreams.exists(s => !outputTypes.contains(s.streamType))) {
      errors += s"Output streams must be one of: ${outputTypes.mkString("[", ", ", "]")}"
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
      errors += s"All t-streams should have the same service"
    } else {
      val service = serviceDAO.get(tStreamsServices.head)
      if (!service.get.isInstanceOf[TStreamService]) {
        errors += s"Service for t-streams must be 'TstrQ'"
      }
    }

    errors
  }
}
