package com.bwsw.sj.crud.rest.validator

import com.bwsw.sj.common.entities.RegularInstanceMetadata

/**
  * Validator for Stream-processing-simple module type
  * Created:  13/04/2016
  *
  * @author Kseniya Tomskikh
  */
class RegularStreamingValidator extends StreamingModuleValidator {

  def validate(options: RegularInstanceMetadata, collectionName: String) = {
    super.validate(options, collectionName)
  }

}
