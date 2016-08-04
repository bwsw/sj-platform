package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.common.ModuleConstants._
import com.bwsw.sj.crud.rest.entities.module.{RegularInstanceMetadata, ModuleSpecification, InstanceMetadata}
import org.slf4j.{LoggerFactory, Logger}

/**
  * Validator for Stream-processing regular module type
  * Created:  13/04/2016
  *
  * @author Kseniya Tomskikh
  */
class RegularStreamingValidator extends StreamingModuleValidator {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  /**
    * Validating input parameters for 'regular-streaming' module
    *
    * @param instanceParameters - input parameters for running module
    * @return - List of errors
    */
  override def validate(instanceParameters: InstanceMetadata, specification: ModuleSpecification) = {
    logger.debug(s"Instance: ${instanceParameters.name}. Start regular-streaming validation.")
    val parameters = instanceParameters.asInstanceOf[RegularInstanceMetadata]
    val result = super.validate(instanceParameters, specification)
    val errors = result._1

    if (!stateManagementModes.contains(parameters.stateManagement)) {
      errors += s"Unknown value of state-management attribute: ${parameters.stateManagement}. " +
        s"State-management must be 'none' or 'ram' or 'rocks'."
    }

    (errors, result._2)
  }
}
