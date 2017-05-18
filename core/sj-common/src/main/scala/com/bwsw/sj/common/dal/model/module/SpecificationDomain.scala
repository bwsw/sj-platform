package com.bwsw.sj.common.dal.model.module

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.PropertyField
import com.bwsw.sj.common.rest.model.module.SpecificationApi

/**
  * Entity for specification-json of module
  *
  * @author Kseniya Tomskikh
  */
class SpecificationDomain(val name: String,
                          val description: String,
                          val version: String,
                          val author: String,
                          val license: String,
                          val inputs: IOstream,
                          val outputs: IOstream,
                          @PropertyField("module-type") val moduleType: String,
                          @PropertyField("engine-name") var engineName: String,
                          @PropertyField("engine-version") var engineVersion: String,
                          @PropertyField("validator-class") val validateClass: String,
                          @PropertyField("executor-class") val executorClass: String,
                          @PropertyField("batch-collector-class") val batchCollectorClass: String) {

  def asSpecification(): SpecificationApi = {
    val serializer = new JsonSerializer
    SpecificationApi(this.name,
      this.description,
      this.version,
      this.author,
      this.license,
      Map("cardinality" -> this.inputs.cardinality,
        "types" -> this.inputs.types),
      Map("cardinality" -> this.outputs.cardinality,
        "types" -> this.outputs.types),
      this.moduleType,
      this.engineName,
      this.engineVersion,
      this.validateClass,
      this.executorClass)
  }
}