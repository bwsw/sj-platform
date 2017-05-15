package com.bwsw.sj.crud.rest.model.module

import com.bwsw.sj.common.dal.model.module.IOstream
import com.bwsw.sj.common.si.model.module.RegularSpecification
import com.bwsw.sj.common.utils.EngineLiterals
import com.fasterxml.jackson.annotation.JsonProperty

class RegularSpecificationApi(name: String,
                              description: String,
                              version: String,
                              author: String,
                              license: String,
                              inputs: IOstream,
                              outputs: IOstream,
                              @JsonProperty("engine-name") engineName: String,
                              @JsonProperty("engine-version") engineVersion: String,
                              options: Map[String, Any],
                              @JsonProperty("validator-class") validatorClass: String,
                              @JsonProperty("executor-class") executorClass: String,
                              @JsonProperty("module-type") moduleType: String = EngineLiterals.regularStreamingType)
  extends SpecificationApi(
    name,
    description,
    version,
    author,
    license,
    inputs,
    outputs,
    moduleType,
    engineName,
    engineVersion,
    options,
    validatorClass,
    executorClass) {

  override def to: RegularSpecification = new RegularSpecification(
    name,
    description,
    version,
    author,
    license,
    inputs,
    outputs,
    engineName,
    engineVersion,
    options,
    validatorClass,
    executorClass)
}
