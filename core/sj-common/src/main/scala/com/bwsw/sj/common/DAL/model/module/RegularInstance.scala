package com.bwsw.sj.common.DAL.model.module

import org.mongodb.morphia.annotations._

/**
 * Entity for regular instance-json
 * Created:  13/04/2016
 *
 * @author Kseniya Tomskikh
 */
class RegularInstance() extends Instance {
  @Property("start-from") var startFrom: String = null
  @Property("state-management") var stateManagement: String = null
  @Property("state-full-checkpoint") var stateFullCheckpoint: Int = 0
  @Property("event-wait-time") var eventWaitTime: Long = 0
}





