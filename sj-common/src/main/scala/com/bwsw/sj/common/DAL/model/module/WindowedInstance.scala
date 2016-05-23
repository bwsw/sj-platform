package com.bwsw.sj.common.DAL.model.module

import org.mongodb.morphia.annotations.Property

/**
 * Entity for windowed instance-json
 * Created:  13/04/2016
  *
  * @author Kseniya Tomskikh
 */
class WindowedInstance() extends Instance {
  @Property("state-management") var stateManagement: String = null
  @Property("state-full-checkpoint") var stateFullCheckpoint: Int = 0
  @Property("time-windowed") var timeWindowed: Int = 0
  @Property("window-full-max") var windowFullMax: Int = 0
}
