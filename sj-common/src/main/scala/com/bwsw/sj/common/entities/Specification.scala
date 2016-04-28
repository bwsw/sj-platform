package com.bwsw.sj.common.entities

import com.fasterxml.jackson.annotation.JsonProperty
import org.mongodb.morphia.annotations.{Property, Embedded}

import scala.collection.immutable.HashMap

@Embedded
class Specification {
  val name: String = null
  val description: String = null
  val version: String = null
  val author: String = null
  val license: String = null
  @Embedded val inputs: IOstream = null
  @Embedded val outputs: IOstream = null
  @Property("module-type") @JsonProperty("module-type") val moduleType: String = null
  val engine: String = null
  @Embedded val options: HashMap[String, Any] = null
  @Property("validator-class") @JsonProperty("validator-class") val validateClass: String = null
  @Property("executor-class") @JsonProperty("executor-class") val executorClass: String = null
}
