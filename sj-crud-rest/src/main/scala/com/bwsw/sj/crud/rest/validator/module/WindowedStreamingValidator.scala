package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.common.DAL.model.{RegularInstance, WindowedInstance}
import com.bwsw.sj.crud.rest.entities.{InstanceMetadata, WindowedInstanceMetadata}

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
  override def validate(instanceParameters: InstanceMetadata, validatedInstance: RegularInstance) = {
    val parameters = instanceParameters.asInstanceOf[WindowedInstanceMetadata]
    val result = super.validate(instanceParameters, validatedInstance)
    val errors = result._1

    if (parameters.timeWindowed <= 0) {
      errors += s"Time-windowed attribute must be > 0"
    }
    if (parameters.windowFullMax <= 0) {
      errors += s"Window-full-max attribute must be > 0"
    }
    var instance = new WindowedInstance
    instance = validatedInstance.asInstanceOf[WindowedInstance]
    instance.timeWindowed = parameters.timeWindowed
    instance.windowFullMax = parameters.windowFullMax
    (errors, instance)
  }

}
