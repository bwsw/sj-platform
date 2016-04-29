package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.Embedded

import scala.collection.immutable.HashMap

/**
 * Entity for execution plan of module instance
 * Created: 14/04/2016
 *
 * @author Kseniya Tomskikh
 */
@Embedded
class ExecutionPlan() {
  var tasks: HashMap[String, Task] = null
}
