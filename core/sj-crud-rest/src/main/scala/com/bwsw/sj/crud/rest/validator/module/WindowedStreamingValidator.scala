package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.utils.EngineConstants
import EngineConstants._
import com.bwsw.sj.crud.rest.entities.module.{WindowedInstanceMetadata, InstanceMetadata, ModuleSpecification}
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
    val parameters = instanceParameters.asInstanceOf[WindowedInstanceMetadata]
    val result = super.validate(instanceParameters, specification)
    val errors = result._1

    if (!stateManagementModes.contains(parameters.stateManagement)) {
      errors += s"Unknown value of state-management attribute: ${parameters.stateManagement}. " +
        s"State-management must be 'none' or 'ram' or 'rocks'."
    }
    if (parameters.timeWindowed <= 0) {
      errors += s"Time-windowed attribute must be > 0"
    }
    if (parameters.windowFullMax <= 0) {
      errors += s"Window-full-max attribute must be > 0"
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
  override protected def validateStreamOptions(parameters: InstanceMetadata,
                                               specification: ModuleSpecification,
                                               errors: ArrayBuffer[String]): (ArrayBuffer[String], Option[Instance]) = ???
}
