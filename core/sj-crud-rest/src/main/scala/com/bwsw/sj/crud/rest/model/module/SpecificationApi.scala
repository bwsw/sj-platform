package com.bwsw.sj.crud.rest.model.module

import com.bwsw.sj.common.dal.model.module.IOstream
import com.bwsw.sj.common.si.model.module._
import com.bwsw.sj.common.utils.EngineLiterals
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSubTypes, JsonTypeInfo}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "module-type", defaultImpl = classOf[SpecificationApi], visible = true)
@JsonSubTypes(Array(
  new Type(value = classOf[InputSpecificationApi], name = EngineLiterals.inputStreamingType),
  new Type(value = classOf[RegularSpecificationApi], name = EngineLiterals.regularStreamingType),
  new Type(value = classOf[BatchSpecificationApi], name = EngineLiterals.batchStreamingType),
  new Type(value = classOf[OutputSpecificationApi], name = EngineLiterals.outputStreamingType)))
class SpecificationApi(val name: String,
                       val description: String,
                       val version: String,
                       val author: String,
                       val license: String,
                       val inputs: IOstream,
                       val outputs: IOstream,
                       @JsonProperty("module-type") val moduleType: String,
                       @JsonProperty("engine-name") val engineName: String,
                       @JsonProperty("engine-version") val engineVersion: String,
                       val options: Map[String, Any],
                       @JsonProperty("validator-class") val validatorClass: String,
                       @JsonProperty("executor-class") val executorClass: String) {

  @JsonIgnore
  def to: Specification = new Specification(
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
    executorClass)
}

object SpecificationApi {
  def from(specification: Specification): SpecificationApi = {
    specification.moduleType match {
      case EngineLiterals.inputStreamingType =>
        val inputSpecification = specification.asInstanceOf[InputSpecification]
        new InputSpecificationApi(
          inputSpecification.name,
          inputSpecification.description,
          inputSpecification.version,
          inputSpecification.author,
          inputSpecification.license,
          inputSpecification.inputs,
          inputSpecification.outputs,
          inputSpecification.engineName,
          inputSpecification.engineVersion,
          inputSpecification.options,
          inputSpecification.validatorClass,
          inputSpecification.executorClass)

      case EngineLiterals.regularStreamingType =>
        val regularSpecification = specification.asInstanceOf[RegularSpecification]
        new RegularSpecificationApi(
          regularSpecification.name,
          regularSpecification.description,
          regularSpecification.version,
          regularSpecification.author,
          regularSpecification.license,
          regularSpecification.inputs,
          regularSpecification.outputs,
          regularSpecification.engineName,
          regularSpecification.engineVersion,
          regularSpecification.options,
          regularSpecification.validatorClass,
          regularSpecification.executorClass)

      case EngineLiterals.batchStreamingType =>
        val batchSpecification = specification.asInstanceOf[BatchSpecification]
        new BatchSpecificationApi(
          batchSpecification.name,
          batchSpecification.description,
          batchSpecification.version,
          batchSpecification.author,
          batchSpecification.license,
          batchSpecification.inputs,
          batchSpecification.outputs,
          batchSpecification.engineName,
          batchSpecification.engineVersion,
          batchSpecification.options,
          batchSpecification.validatorClass,
          batchSpecification.executorClass,
          batchSpecification.batchCollectorClass)

      case EngineLiterals.outputStreamingType =>
        val outputSpecification = specification.asInstanceOf[OutputSpecification]
        new OutputSpecificationApi(
          outputSpecification.name,
          outputSpecification.description,
          outputSpecification.version,
          outputSpecification.author,
          outputSpecification.license,
          outputSpecification.inputs,
          outputSpecification.outputs,
          outputSpecification.engineName,
          outputSpecification.engineVersion,
          outputSpecification.options,
          outputSpecification.validatorClass,
          outputSpecification.executorClass,
          outputSpecification.entityClass)

      case _ =>
        new SpecificationApi(
          specification.name,
          specification.description,
          specification.version,
          specification.author,
          specification.license,
          specification.inputs,
          specification.outputs,
          specification.moduleType,
          specification.engineName,
          specification.engineVersion,
          specification.options,
          specification.validatorClass,
          specification.executorClass)
    }
  }
}
