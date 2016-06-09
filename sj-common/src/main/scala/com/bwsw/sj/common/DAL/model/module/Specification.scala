package com.bwsw.sj.common.DAL.model.module

import org.mongodb.morphia.annotations.Property

/**
  * Entity for specification-json of module
  * Created:  28/04/2016
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
  @Property("engine-name")var engineName: String = null
  @Property("engine-version")var engineVersion: String = null
  val options: String = null
  @Property("validator-class") val validateClass: String = null
  @Property("executor-class") val executorClass: String = null
}
