package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.crud.rest.entities.module.{ModuleSpecification, InstanceMetadata}

/**
  * Created: 23/05/2016
  *
  * @author Kseniya Tomskikh
  */
class OutputStreamingValidator extends StreamingModuleValidator {

  override def validate(parameters: InstanceMetadata, specification: ModuleSpecification, validatedInstance: Instance) = {
    super.validate(parameters, specification, validatedInstance)
  }

}
