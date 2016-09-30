package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.common.rest.entities.module.{InstanceMetadata, SpecificationData, WindowedInstanceMetadata}
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.EngineLiterals._
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ArrayBuffer

/**
 * Validator for Stream-processing-windowed module type
 *
 * @author Kseniya Tomskikh
 */
class WindowedStreamingValidator extends StreamingModuleValidator {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  override def validate(parameters: InstanceMetadata, specification: SpecificationData) = {
    logger.debug(s"Instance: ${parameters.name}. Start windowed-streaming validation.")
    val errors = new ArrayBuffer[String]()
    errors ++= super.validateGeneralOptions(parameters)
    val windowedInstanceMetadata = parameters.asInstanceOf[WindowedInstanceMetadata]

    // 'checkpoint-mode' field
    Option(windowedInstanceMetadata.checkpointMode) match {
      case None =>
        errors += s"'Checkpoint-mode' is required"
      case Some(x) =>
        if (!checkpointModes.contains(windowedInstanceMetadata.checkpointMode)) {
          errors += s"Unknown value of checkpoint-mode attribute: ${windowedInstanceMetadata.checkpointMode}."
        }
    }

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

    // 'time-windowed' field
    if (windowedInstanceMetadata.timeWindowed <= 0) {
      errors += s"Time-windowed attribute must be greater than zero"
    }

    // 'window-full-max' field
    if (windowedInstanceMetadata.windowFullMax <= 0) {
      errors += s"Window-full-max attribute must be greater than zero"
    }

    errors ++= validateStreamOptions(windowedInstanceMetadata, specification)

    errors
  }

  def validateStreamOptions(parameters: InstanceMetadata,
                            specification: SpecificationData): ArrayBuffer[String] = ???
}
