package com.bwsw.sj.crud.rest.validator.module


import com.bwsw.sj.crud.rest.entities.{TimeWindowedInstanceMetadata, InstanceMetadata}

/**
  * Validator for Stream-processing-windowed module type
  * Created:  13/04/2016
  *
  * @author Kseniya Tomskikh
  */
class TimeWindowedStreamingValidator extends StreamingModuleValidator {

  /**
    * Validating input parameters for 'time-windowed-streaming' module
 *
    * @param parameters - input parameters for running module
    * @return - List of errors
    */
  def validate(parameters: TimeWindowedInstanceMetadata) = {
    val validateResult = super.validate(parameters)
    var errors = validateResult._1
    if (parameters.timeWindowed <= 0) {
      errors += s"Time-windowed attribute must be > 0"
    }
    if (parameters.windowFullMax <= 0) {
      errors += s"Window-full-max attribute must be > 0"
    }
    (errors, validateResult._2, validateResult._3)
  }

}
