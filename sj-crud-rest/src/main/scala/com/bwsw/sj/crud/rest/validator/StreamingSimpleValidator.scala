package com.bwsw.sj.crud.rest.validator

import com.bwsw.sj.common.entities.SimpleInstanceMetadata

/**
  * Validator for Stream-processing-simple module type
  * Created:  13/04/2016
  *
  * @author Kseniya Tomskikh
  */
class StreamingSimpleValidator extends StreamingModuleValidator {

  def validate(options: SimpleInstanceMetadata) = {
    super.validate(options)
  }

}
