package com.bwsw.sj.crud.rest.instance.validator

import com.bwsw.sj.common.dal.model.service.TStreamServiceDomain
import com.bwsw.sj.common.si.model.instance.InputInstance
import com.bwsw.sj.common.si.model.module.Specification
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.StreamLiterals.tstreamType
import org.slf4j.{Logger, LoggerFactory}
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

/**
  * Validator for input-streaming instance
  *
  * @author Kseniya Tomskikh
  */
class InputInstanceValidator(implicit injector: Injector) extends InstanceValidator {

  import messageResourceUtils.createMessage

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  override type T = InputInstance

  /**
    * Validating input parameters for input-streaming module
    *
    * @param instance - input parameters for running module
    * @return - List of errors
    */
  override def validate(instance: T, specification: Specification): Seq[String] = {
    logger.debug(s"Instance: ${instance.name}. Start a validation of instance of input-streaming type.")
    val errors = new ArrayBuffer[String]()
    errors ++= super.validate(instance, specification)

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
            errors += createMessage("rest.validator.attribute.unknown.value", "checkpointMode", s"$x") + ". " +
              createMessage("rest.validator.attribute.must.one_of", "checkpointMode", checkpointModes.mkString("[", ", ", "]"))
          }
        }
    }

    // 'checkpoint-interval' field
    if (instance.checkpointInterval <= 0) {
      errors += createMessage("rest.validator.attribute.required", "checkpointInterval") + ". " +
        createMessage("rest.validator.attribute.must.greater.than.zero", "checkpointInterval")
    }

    if (instance.lookupHistory < 0) {
      errors += createMessage("rest.validator.attribute.required", "lookupHistory") + ". " +
        createMessage("rest.validator.attribute.must.greater.or.equal.zero", "lookupHistory")
    }

    if (instance.queueMaxSize < 271) {
      errors += createMessage("rest.validator.attribute.required", "queueMaxSize") + ". " +
        createMessage("rest.validator.attribute.must.greater.or.equal", "queueMaxSize", "271")
    }

    if (!defaultEvictionPolicies.contains(instance.defaultEvictionPolicy)) {
      errors += createMessage("rest.validator.attribute.unknown.value", "defaultEvictionPolicy", instance.defaultEvictionPolicy) + ". " +
        createMessage("rest.validator.attribute.must.one_of", "defaultEvictionPolicy", defaultEvictionPolicies.mkString("[", ", ", "]"))
    }

    if (!evictionPolicies.contains(instance.evictionPolicy)) {
      errors += createMessage("rest.validator.attribute.unknown.value", "evictionPolicy", instance.evictionPolicy) + ". " +
        createMessage("rest.validator.attribute.must.one_of", "evictionPolicy", evictionPolicies.mkString("[", ", ", "]"))
    }

    if (instance.backupCount < 0 || instance.backupCount > 6)
      errors += createMessage("rest.validator.attribute.must.from.to", "backupCount", "0", "6")

    if (instance.asyncBackupCount < 0) {
      errors += createMessage("rest.validator.attribute.must.greater.or.equal.zero", "asyncBackupCount")
    }

    errors
  }

  override protected def validateStreamOptions(instance: T, specification: Specification): Seq[String] = {
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

  private def checkBackupNumber(parameters: T, errors: ArrayBuffer[String]) = {
    val parallelism = parameters.parallelism.asInstanceOf[Int]
    if (parallelism <= 0) {
      errors += createMessage("rest.validator.attribute.must.greater.than.zero", "Parallelism")
    }
    if (parallelism <= (parameters.backupCount + parameters.asyncBackupCount)) {
      errors += createMessage("rest.validator.attribute.must.greater.than.backups", "Parallelism")
    }
  }
}
