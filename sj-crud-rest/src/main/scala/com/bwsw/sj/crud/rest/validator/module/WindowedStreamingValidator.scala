package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.common.DAL.model.module.{Instance, WindowedInstance}
import com.bwsw.sj.common.ModuleConstants._
import com.bwsw.sj.crud.rest.entities.module.{WindowedInstanceMetadata, InstanceMetadata, ModuleSpecification}

/**
  * Validator for Stream-processing-windowed module type
  * Created:  13/04/2016
  *
  * @author Kseniya Tomskikh
  */
class WindowedStreamingValidator extends StreamingModuleValidator {

  /**
    * Validating input parameters for 'windowed-streaming' module
    *
    * @param instanceParameters - input parameters for running module
    * @return - List of errors
    */
  override def validate(instanceParameters: InstanceMetadata, specification: ModuleSpecification) = {
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

}
