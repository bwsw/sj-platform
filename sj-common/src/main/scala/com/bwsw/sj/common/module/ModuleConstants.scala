package com.bwsw.sj.common.module

/**
  * Object with constants for modules
  * Created: 13/04/2016
  * @author Kseniya Tomskikh
  */
object ModuleConstants {

  val timeWindowedType = "time-windowed-streaming"
  val regularStreamingType = "regular-streaming"

  val moduleTypes = Set(timeWindowedType, regularStreamingType)
  val checkpointModes = Set("every-nth", "time-interval")
  val stateManagementModes = Set("none", "ram", "rocks")
  val startFromModes = Set("oldest", "newest")

  val ready = "ready"
  val started = "started"
  val stopped = "stopped"
  val instanceStatusModes = Set(started, stopped, ready)

}
