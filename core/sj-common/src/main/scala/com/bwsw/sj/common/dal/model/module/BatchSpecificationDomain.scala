package com.bwsw.sj.common.dal.model.module

import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.PropertyField

class BatchSpecificationDomain(name: String,
                               description: String,
                               version: String,
                               author: String,
                               license: String,
                               inputs: IOstream,
                               outputs: IOstream,
                               @PropertyField("module-type") moduleType: String,
                               @PropertyField("engine-name") engineName: String,
                               @PropertyField("engine-version") engineVersion: String,
                               @PropertyField("validator-class") validateClass: String,
                               @PropertyField("executor-class") executorClass: String,
                               @PropertyField("batch-collector-class") val batchCollectorClass: String)
  extends SpecificationDomain(
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
    validateClass,
    executorClass) {

}
