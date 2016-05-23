package com.bwsw.sj.common.DAL.model.module

import org.mongodb.morphia.annotations.Property

class Specification {
  val name: String = null
  val description: String = null
  val version: String = null
  val author: String = null
  val license: String = null
  val inputs: IOstream = null
  val outputs: IOstream = null
  @Property("module-type") val moduleType: String = null
  val engine: String = null
  val options: String = null
  @Property("validator-class") val validateClass: String = null
  @Property("executor-class") val executorClass: String = null
}
