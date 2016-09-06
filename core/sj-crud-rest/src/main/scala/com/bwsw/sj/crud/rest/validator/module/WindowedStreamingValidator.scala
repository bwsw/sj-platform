package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.utils.EngineConstants
import EngineConstants._
import com.bwsw.sj.common.rest.entities.module.{WindowedInstanceMetadata, InstanceMetadata, ModuleSpecification}
import org.slf4j.{LoggerFactory, Logger}

import scala.collection.mutable.ArrayBuffer

/**
  * Validator for Stream-processing-windowed module type
  *
  * @author Kseniya Tomskikh
  */
class WindowedStreamingValidator extends StreamingModuleValidator {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  /**
    * Validating input parameters for 'windowed-streaming' module
    *
    * @param instanceParameters - input parameters for running module
    * @return - List of errors
    */
  override def validate(instanceParameters: InstanceMetadata, specification: ModuleSpecification) = {
    logger.debug(s"Instance: ${instanceParameters.name}. Start windowed-streaming validation.")
    val windowedInstanceMetadata = instanceParameters.asInstanceOf[WindowedInstanceMetadata]
    val generalErrors = super.validateGeneralOptions(windowedInstanceMetadata)
    val result = validateStreamOptions(windowedInstanceMetadata, specification, generalErrors)
    val errors = result._1

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
        s"State-management must be one of: ${stateManagementModes.mkString("[", ",", "]")}"
    } else {
      if (windowedInstanceMetadata.stateManagement != "none") {
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
    (errors, result._2)
  }

  /**
   * Validating options of streams of instance for module
   *
   * @param parameters - Input instance parameters
   * @param specification - Specification of module
   * @param errors - List of validating errors
   * @return - List of errors and validating instance (null, if errors non empty)
   */
  def validateStreamOptions(parameters: InstanceMetadata,
                                               specification: ModuleSpecification,
                                               errors: ArrayBuffer[String]): (ArrayBuffer[String], Option[Instance]) = ???
}
