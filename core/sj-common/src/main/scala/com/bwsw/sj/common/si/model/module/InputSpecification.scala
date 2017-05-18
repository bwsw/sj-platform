package com.bwsw.sj.common.si.model.module

import com.bwsw.sj.common.dal.model.module.IOstream
import com.bwsw.sj.common.utils.EngineLiterals

class InputSpecification(name: String,
                         description: String,
                         version: String,
                         author: String,
                         license: String,
                         inputs: IOstream,
                         outputs: IOstream,
                         engineName: String,
                         engineVersion: String,
                         validatorClass: String,
                         executorClass: String)
  extends Specification(
    name,
    description,
    version,
    author,
    license,
    inputs,
    outputs,
    EngineLiterals.inputStreamingType,
    engineName,
    engineVersion,
    validatorClass,
    executorClass) {

}


