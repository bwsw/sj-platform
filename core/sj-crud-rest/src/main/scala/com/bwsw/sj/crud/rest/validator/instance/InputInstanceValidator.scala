package com.bwsw.sj.crud.rest.validator.instance

import com.bwsw.sj.common.DAL.model.TStreamService
import com.bwsw.sj.common.rest.entities.module.{InputInstanceMetadata, InstanceMetadata, SpecificationData}
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.ServiceLiterals
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ArrayBuffer

/**
  * Validator for input-streaming instance
  *
  * @author Kseniya Tomskikh
  */
class InputInstanceValidator extends InstanceValidator {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  /**
    * Validating input parameters for input-streaming module
    *
    * @param parameters - input parameters for running module
    * @return - List of errors
    */
  override def validate(parameters: InstanceMetadata,
                        specification: SpecificationData) = {
    logger.debug(s"Instance: ${parameters.name}. Start input-streaming validation.")
    val errors = new ArrayBuffer[String]()
    errors ++= super.validateGeneralOptions(parameters)
    val inputInstanceMetadata = parameters.asInstanceOf[InputInstanceMetadata]

    // 'checkpoint-mode' field
    Option(inputInstanceMetadata.checkpointMode) match {
      case None =>
        errors += createMessage("rest.validator.attribute.required", "Checkpoint-mode")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("rest.validator.attribute.required", "Checkpoint-mode")
        }
        else {
          if (!checkpointModes.contains(x)) {
            errors += createMessage("rest.validator.attribute.unknown.value", "checkpoint-mode", s"$x") + ". " +
              createMessage("rest.validator.attribute.must.one_of", "Checkpoint-mode", checkpointModes.mkString("[", ", ", "]"))
          }
        }
    }

    // 'checkpoint-interval' field
    if (inputInstanceMetadata.checkpointInterval <= 0) {
      errors += createMessage("rest.validator.attribute.required", "Checkpoint-interval") + ". " +
        createMessage("rest.validator.attribute.must.greater.than.zero", "Checkpoint-interval")
    }

    if (inputInstanceMetadata.lookupHistory < 0) {
      errors += createMessage("rest.validator.attribute.required", "Lookup-history") + ". " +
        createMessage("rest.validator.attribute.must.greater.or.equal.zero", "Lookup-history")
    }

    if (inputInstanceMetadata.queueMaxSize < 0) {
      errors += createMessage("rest.validator.attribute.required", "Queue-max-size") + ". " +
        createMessage("rest.validator.attribute.must.greater.or.equal.zero", "Queue-max-size")
    }

    if (!defaultEvictionPolicies.contains(inputInstanceMetadata.defaultEvictionPolicy)) {
      errors += createMessage("rest.validator.attribute.unknown.value", "default-eviction-policy", inputInstanceMetadata.defaultEvictionPolicy) + ". " +
        createMessage("rest.validator.attribute.must.one_of", "Default-eviction-policy", defaultEvictionPolicies.mkString("[", ", ", "]"))
    }

    if (!evictionPolicies.contains(inputInstanceMetadata.evictionPolicy)) {
      errors += createMessage("rest.validator.attribute.unknown.value", "eviction-policy", inputInstanceMetadata.evictionPolicy) + ". " +
        createMessage("rest.validator.attribute.must.one_of", "Eviction-policy", evictionPolicies.mkString("[", ", ", "]"))
    }

    if (inputInstanceMetadata.backupCount < 0 || inputInstanceMetadata.backupCount > 6)
      errors += createMessage("rest.validator.attribute.must.from.to", "Backup-count", "0", "6")

    if (inputInstanceMetadata.asyncBackupCount < 0) {
      errors += createMessage("rest.validator.attribute.must.greater.or.equal.zero", "Async-backup-count")
    }

    errors ++= validateStreamOptions(inputInstanceMetadata, specification)

    errors
  }

  def validateStreamOptions(instance: InputInstanceMetadata,
                            specification: SpecificationData) = {
    logger.debug(s"Instance: ${instance.name}. Stream options validation.")
    val errors = new ArrayBuffer[String]()

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

    if (outputStreams.nonEmpty && !outputStreams.exists(_.streamType != ServiceLiterals.tstreamsType)) {
      val tStreamsServices = getStreamServices(outputStreams)
      if (tStreamsServices.size != 1) {
        errors += createMessage("rest.validator.t_stream.same.service")
      } else {
        val service = serviceDAO.get(tStreamsServices.head)
        if (!service.get.isInstanceOf[TStreamService]) {
          errors += createMessage("rest.validator.service.must", "t-streams", "TstrQ")
        }
      }
    }

    // 'parallelism' field
    Option(instance.parallelism) match {
      case None =>
        errors += createMessage("rest.validator.attribute.required", "Parallelism")
      case Some(x) =>
        x match {
          case dig: Int =>
            checkBackupNumber(instance, errors)
          case _ =>
            errors += createMessage("rest.validator.parameter.unknown.type", "parallelism", "digit")
        }
    }

    errors
  }

  private def checkBackupNumber(parameters: InputInstanceMetadata, errors: ArrayBuffer[String]) = {
    val parallelism = parameters.parallelism.asInstanceOf[Int]
    if (parallelism <= 0) {
      errors += createMessage("rest.validator.attribute.must.greater.than.zero", "Parallelism")
    }
    if (parallelism <= (parameters.backupCount + parameters.asyncBackupCount)) {
      errors += createMessage("rest.validator.attribute.must.greater.than.backups", "Parallelism")
    }
  }
}
