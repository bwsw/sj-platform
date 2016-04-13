package com.bwsw.sj.crud.rest.validator

import com.bwsw.sj.common.entities.WindowedInstanceMetadata

/**
  * Validator for Stream-processing-windowed module type
  * Created:  13/04/2016
  *
  * @author Kseniya Tomskikh
  */
class TimeWindowedStreamingValidator extends StreamingModuleValidator {
  def validate(options: WindowedInstanceMetadata, collectionName: String) = {
    super.validate(options, collectionName)
  }
}
