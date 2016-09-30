package com.bwsw.sj.common.DAL.model.module

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.rest.entities.module.SpecificationData
import org.mongodb.morphia.annotations.Property

/**
 * Entity for specification-json of module
 *
 * @author Kseniya Tomskikh
 */
class Specification {
  val name: String = null
  val description: String = null
  val version: String = null
  val author: String = null
  val license: String = null
  val inputs: IOstream = null
  val outputs: IOstream = null
  @Property("module-type") val moduleType: String = null
  @Property("engine-name") var engineName: String = null
  @Property("engine-version") var engineVersion: String = null
  val options: String = "{}"
  @Property("validator-class") val validateClass: String = null
  @Property("executor-class") val executorClass: String = null
  @Property("entity-class") val entityClass: String = null

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