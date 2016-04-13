package com.bwsw.sj.crud.rest.validator

import com.bwsw.sj.common.entities.WindowedInstanceMetadata

/**
  * Validator for Stream-processing-windowed module type
  * Created:  13/04/2016
  *
  * @author Kseniya Tomskikh
  */
class StreamingWindowedValidator extends StreamingModuleValidator {
  def validate(options: WindowedInstanceMetadata) = {
    super.validate(options)
  }
}
