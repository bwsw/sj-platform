package com.bwsw.sj.common.DAL.model.module

import org.mongodb.morphia.annotations.Property

/**
  * Entity for output-streaming instance-json
  * Created: 23/05/2016
  *
  * @author Kseniya Tomskikh
  */
class OutputInstance() extends Instance {
  var inputs: Array[String] = null
  @Property("start-from") var startFrom: String = null
}
