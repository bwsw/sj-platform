package com.bwsw.sj.crud.rest.validator.instance

import com.bwsw.sj.common.DAL.model.TStreamService
import com.bwsw.sj.common.rest.entities.module.{InputInstanceMetadata, InstanceMetadata, SpecificationData}
import com.bwsw.sj.common.utils.EngineLiterals._
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ArrayBuffer

/**
 * Validator for input-streaming instance
 *
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
        errors += s"'Checkpoint-mode' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'Checkpoint-mode' is required"
        }
        else {
          if (!checkpointModes.contains(x)) {
            errors += s"Unknown value of 'checkpoint-mode' attribute: '$x'. " +
              s"'Checkpoint-mode' must be one of: ${checkpointModes.mkString("[", ", ", "]")}"
          }
        }
    }

    // 'checkpoint-interval' field
    if (inputInstanceMetadata.checkpointInterval <= 0) {
      errors += s"'Checkpoint-interval' must be greater than zero"
    }

    if (inputInstanceMetadata.lookupHistory < 0) {
      errors += s"'Lookup-history' attribute must be greater than zero or equal to zero"
    }

    if (inputInstanceMetadata.queueMaxSize < 0) {
      errors += s"'Queue-max-size' attribute must be greater than zero or equal to zero"
    }

    if (!defaultEvictionPolicies.contains(inputInstanceMetadata.defaultEvictionPolicy)) {
      errors += s"Unknown value of 'default-eviction-policy' attribute: '${inputInstanceMetadata.defaultEvictionPolicy}'. " +
        s"'Default-eviction-policy' must be one of: ${defaultEvictionPolicies.mkString("[", ", ", "]")}"
    }

    if (!evictionPolicies.contains(inputInstanceMetadata.evictionPolicy)) {
      errors += s"Unknown value of 'eviction-policy' attribute: '${inputInstanceMetadata.evictionPolicy}'. " +
        s"'Eviction-policy' must be one of: ${evictionPolicies.mkString("[", ", ", "]")}"
    }

    if (inputInstanceMetadata.backupCount < 0 || inputInstanceMetadata.backupCount > 6)
      errors += "'Backup-count' must be in the interval from 0 to 6"

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
      errors += s"Count of outputs cannot be less than ${outputsCardinality(0)}"
    }
    if (instance.outputs.length > outputsCardinality(1)) {
      errors += s"Count of outputs cannot be more than ${outputsCardinality(1)}"
    }
    if (doesContainDoubles(instance.outputs)) {
      errors += s"'Outputs' contain the non-unique streams"
    }
    val outputStreams = getStreams(instance.outputs)
    instance.outputs.toList.foreach { streamName =>
      if (!outputStreams.exists(s => s.name == streamName)) {
        errors += s"Output stream '$streamName' does not exist"
      }
    }
    val outputTypes = specification.outputs("types").asInstanceOf[Array[String]]
    if (outputStreams.exists(s => !outputTypes.contains(s.streamType))) {
      errors += s"Output streams must be one of the following type: ${outputTypes.mkString("[", ", ", "]")}"
    }

    if (outputStreams.nonEmpty) {
      val tStreamsServices = getStreamServices(outputStreams)
      if (tStreamsServices.size != 1) {
        errors += s"All t-streams should have the same service"
      } else {
        val service = serviceDAO.get(tStreamsServices.head)
        if (!service.get.isInstanceOf[TStreamService]) {
          errors += s"Service for t-streams must be 'TstrQ'"
        }
      }

      // 'parallelism' field
      Option(instance.parallelism) match {
        case None =>
          errors += s"'Parallelism' is required"
        case Some(x) =>
          x match {
            case dig: Int =>
              checkBackupNumber(instance, errors)
            case _ =>
              errors += "Unknown type of 'parallelism' parameter. Must be a digit"
          }
      }
    }

    errors
  }

  private def checkBackupNumber(parameters: InputInstanceMetadata, errors: ArrayBuffer[String]) = {
    val parallelism = parameters.parallelism.asInstanceOf[Int]
    if (parallelism <= 0) {
      errors += "'Parallelism' must be greater than zero"
    }
    if (parallelism <= (parameters.backupCount + parameters.asyncBackupCount)) {
      errors += "'Parallelism' must be greater than the total number of backups"
    }
  }
}
