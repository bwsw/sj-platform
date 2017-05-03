package com.bwsw.sj.common.DAL.model.module

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.morphia.MorphiaAnnotations.PropertyField
import com.bwsw.sj.common.rest.entities.module.SpecificationData

/**
  * Entity for specification-json of module
  *
  * @author Kseniya Tomskikh
  */
class Specification(val name: String,
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
                    @PropertyField("batch-collector-class") val batchCollectorClass: String,
                    val options: String = "{}"
                   ) {

  def asSpecificationData() = {
    val serializer = new JsonSerializer
    SpecificationData(this.name,
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
      serializer.deserialize[Map[String, Any]](this.options),
      this.validateClass,
      this.executorClass)
  }
}