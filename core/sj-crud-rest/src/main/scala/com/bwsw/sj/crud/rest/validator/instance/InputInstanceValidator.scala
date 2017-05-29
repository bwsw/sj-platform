package com.bwsw.sj.crud.rest.validator.instance

import com.bwsw.sj.common.dal.model.service.TStreamServiceDomain
import com.bwsw.sj.common.si.model.instance.{InputInstance, Instance}
import com.bwsw.sj.common.si.model.module.Specification
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.StreamLiterals.tstreamType
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
    * @param instance - input parameters for running module
    * @return - List of errors
    */
  override def validate(instance: Instance, specification: Specification) = {
    logger.debug(s"Instance: ${instance.name}. Start a validation of instance of input-streaming type.")
    val errors = new ArrayBuffer[String]()
    errors ++= super.validateGeneralOptions(instance)
    val inputInstanceMetadata = instance.asInstanceOf[InputInstance]

    // 'checkpoint-mode' field
    Option(inputInstanceMetadata.checkpointMode) match {
      case None =>
        errors += createMessage("rest.validator.attribute.required", "checkpointMode")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("rest.validator.attribute.required", "checkpointMode")
        }
        else {
          if (!checkpointModes.contains(x)) {
            errors += createMessage("rest.validator.attribute.unknown.value", "checkpointMode", s"$x") + ". " +
              createMessage("rest.validator.attribute.must.one_of", "checkpointMode", checkpointModes.mkString("[", ", ", "]"))
          }
        }
    }

    // 'checkpoint-interval' field
    if (inputInstanceMetadata.checkpointInterval <= 0) {
      errors += createMessage("rest.validator.attribute.required", "checkpointInterval") + ". " +
        createMessage("rest.validator.attribute.must.greater.than.zero", "checkpointInterval")
    }

    if (inputInstanceMetadata.lookupHistory < 0) {
      errors += createMessage("rest.validator.attribute.required", "lookupHistory") + ". " +
        createMessage("rest.validator.attribute.must.greater.or.equal.zero", "lookupHistory")
    }

    if (inputInstanceMetadata.queueMaxSize < 271) {
      errors += createMessage("rest.validator.attribute.required", "queueMaxSize") + ". " +
        createMessage("rest.validator.attribute.must.greater.or.equal", "queueMaxSize", "271")
    }

    if (!defaultEvictionPolicies.contains(inputInstanceMetadata.defaultEvictionPolicy)) {
      errors += createMessage("rest.validator.attribute.unknown.value", "defaultEvictionPolicy", inputInstanceMetadata.defaultEvictionPolicy) + ". " +
        createMessage("rest.validator.attribute.must.one_of", "defaultEvictionPolicy", defaultEvictionPolicies.mkString("[", ", ", "]"))
    }

    if (!evictionPolicies.contains(inputInstanceMetadata.evictionPolicy)) {
      errors += createMessage("rest.validator.attribute.unknown.value", "evictionPolicy", inputInstanceMetadata.evictionPolicy) + ". " +
        createMessage("rest.validator.attribute.must.one_of", "evictionPolicy", evictionPolicies.mkString("[", ", ", "]"))
    }

    if (inputInstanceMetadata.backupCount < 0 || inputInstanceMetadata.backupCount > 6)
      errors += createMessage("rest.validator.attribute.must.from.to", "backupCount", "0", "6")

    if (inputInstanceMetadata.asyncBackupCount < 0) {
      errors += createMessage("rest.validator.attribute.must.greater.or.equal.zero", "asyncBackupCount")
    }

    errors ++= validateStreamOptions(inputInstanceMetadata, specification)

    errors
  }

  def validateStreamOptions(instance: InputInstance, specification: Specification) = {
    logger.debug(s"Instance: ${instance.name}. Stream options validation.")
    val errors = new ArrayBuffer[String]()

    // 'outputs' field
    val outputsCardinality = specification.outputs.cardinality
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
    val outputTypes = specification.outputs.types
    if (outputStreams.exists(s => !outputTypes.contains(s.streamType))) {
      errors += createMessage("rest.validator.source_stream.must.one.of", "Output", outputTypes.mkString("[", ", ", "]"))
    }

    val tStreamsServices = getStreamServices(outputStreams.filter { s =>
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

    // 'parallelism' field
    Option(instance.parallelism) match {
      case None =>
        errors += createMessage("rest.validator.attribute.required", "Parallelism")
      case Some(x) =>
        x match {
          case _: Int =>
            checkBackupNumber(instance, errors)
          case _ =>
            errors += createMessage("rest.validator.parameter.unknown.type", "parallelism", "digit")
        }
    }

    errors
  }

  private def checkBackupNumber(parameters: InputInstance, errors: ArrayBuffer[String]) = {
    val parallelism = parameters.parallelism.asInstanceOf[Int]
    if (parallelism <= 0) {
      errors += createMessage("rest.validator.attribute.must.greater.than.zero", "Parallelism")
    }
    if (parallelism <= (parameters.backupCount + parameters.asyncBackupCount)) {
      errors += createMessage("rest.validator.attribute.must.greater.than.backups", "Parallelism")
    }
  }
}
