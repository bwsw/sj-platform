package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.crud.rest.entities.WindowedInstanceMetadata

/**
  * Validator for Stream-processing-windowed module type
  * Created:  13/04/2016
  *
  * @author Kseniya Tomskikh
  */
class WindowedStreamingValidator extends StreamingModuleValidator {

  /**
    * Validating input parameters for 'windowed-streaming' module
    * @param parameters - input parameters for running module
    * @return - List of errors
    */
  def validate(parameters: WindowedInstanceMetadata) = {
    val (errors, validatedInstance) = super.validate(parameters)
    if (parameters.timeWindowed <= 0) {
      errors += s"Time-windowed attribute must be > 0"
    }
    if (parameters.windowFullMax <= 0) {
      errors += s"Window-full-max attribute must be > 0"
    }
    val instance = validatedInstance.asInstanceOf[WindowedInstanceMetadata]
    instance.timeWindowed = parameters.timeWindowed
    instance.windowFullMax = parameters.windowFullMax
    (errors, instance)
  }

}
