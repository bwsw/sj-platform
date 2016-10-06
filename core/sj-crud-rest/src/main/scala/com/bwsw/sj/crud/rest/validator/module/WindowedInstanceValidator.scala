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

    errors ++= validateStreamOptions(windowedInstanceMetadata, specification)

    errors
  }

  def validateStreamOptions(parameters: InstanceMetadata,
                            specification: SpecificationData): ArrayBuffer[String] = ???
}
