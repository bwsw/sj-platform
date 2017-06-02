package com.bwsw.sj.common.si.model.module

import com.bwsw.sj.common.dal.model.module.{BatchSpecificationDomain, IOstream}
import com.bwsw.sj.common.utils.EngineLiterals
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

class BatchSpecification(name: String,
                         description: String,
                         version: String,
                         author: String,
                         license: String,
                         inputs: IOstream,
                         outputs: IOstream,
                         engineName: String,
                         engineVersion: String,
                         validatorClass: String,
                         executorClass: String,
                         val batchCollectorClass: String)
                        (implicit injector: Injector)
  extends Specification(
    name,
    description,
    version,
    author,
    license,
    inputs,
    outputs,
    EngineLiterals.batchStreamingType,
    engineName,
    engineVersion,
    validatorClass,
    executorClass) {

  import messageResourceUtils.createMessage

  override def to: BatchSpecificationDomain = {
    new BatchSpecificationDomain(
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
      executorClass,
      batchCollectorClass)
  }

  override def validate: ArrayBuffer[String] = {
    val errors = validateGeneralFields

    Option(batchCollectorClass) match {
      case None | Some("") =>
        errors += createMessage("rest.validator.specification.attribute.required", "batch-collector-class")
      case _ =>
    }

    errors
  }
}


