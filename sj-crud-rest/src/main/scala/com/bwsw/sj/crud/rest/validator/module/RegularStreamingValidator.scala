package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.common.DAL.model.RegularInstance
import com.bwsw.sj.crud.rest.entities.InstanceMetadata

/**
  * Validator for Stream-processing-simple module type
  * Created:  13/04/2016
  *
  * @author Kseniya Tomskikh
  */
class RegularStreamingValidator extends StreamingModuleValidator {

  /**
    * Validating input parameters for 'regular-streaming' module
 *
    * @param parameters - input parameters for running module
    * @return - List of errors
    */
  override def validate(parameters: InstanceMetadata, validatedInstance: RegularInstance) = {
    super.validate(parameters, validatedInstance)
  }
}
