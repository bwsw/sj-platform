package com.bwsw.sj.common.DAL.model.module

import org.mongodb.morphia.annotations.Property

/**
  * Entity for output-streaming instance-json
  *
  *
  * @author Kseniya Tomskikh
  */
class OutputInstance() extends Instance {
  @Property("start-from") var startFrom: String = null
}
