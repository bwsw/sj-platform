package com.bwsw.sj.common.si.model.module

import com.bwsw.sj.common.dal.model.module.IOstream
import com.bwsw.sj.common.utils.EngineLiterals

class OutputSpecification(name: String,
                          description: String,
                          version: String,
                          author: String,
                          license: String,
                          inputs: IOstream,
                          outputs: IOstream,
                          engineName: String,
                          engineVersion: String,
                          options: Map[String, Any],
                          validatorClass: String,
                          executorClass: String,
                          val entityClass: String)
  extends Specification(
    name,
    description,
    version,
    author,
    license,
    inputs,
    outputs,
    EngineLiterals.outputStreamingType,
    engineName,
    engineVersion,
    options,
    validatorClass,
    executorClass) {

}


