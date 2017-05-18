package com.bwsw.sj.crud.rest.model.module

import com.bwsw.sj.common.dal.model.module.IOstream
import com.bwsw.sj.common.si.model.module.BatchSpecification
import com.bwsw.sj.common.utils.EngineLiterals
import com.fasterxml.jackson.annotation.JsonProperty

class BatchSpecificationApi(name: String,
                            description: String,
                            version: String,
                            author: String,
                            license: String,
                            inputs: IOstream,
                            outputs: IOstream,
                            @JsonProperty("engine-name") engineName: String,
                            @JsonProperty("engine-version") engineVersion: String,
                            @JsonProperty("validator-class") validatorClass: String,
                            @JsonProperty("executor-class") executorClass: String,
                            @JsonProperty("batch-collector-class") val batchCollectorClass: String,
                            @JsonProperty("module-type") moduleType: String = EngineLiterals.batchStreamingType)
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
    validatorClass,
    executorClass) {

  override def to: BatchSpecification = new BatchSpecification(
    name,
    description,
    version,
    author,
    license,
    inputs,
    outputs,
    engineName,
    engineVersion,
    validatorClass,
    executorClass,
    batchCollectorClass)
}
