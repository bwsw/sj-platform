package com.bwsw.sj.common.DAL.model.module

import com.bwsw.sj.common.rest.entities.module.{InstanceMetadata, WindowedInstanceMetadata}
import org.mongodb.morphia.annotations.{Embedded, Property}

/**
 * Entity for windowed instance-json
  *
  * @author Kseniya Tomskikh
 */
class WindowedInstance() extends Instance {
  @Embedded("execution-plan") var executionPlan: ExecutionPlan = null
  @Property("start-from") var startFrom: String = null
  @Property("state-management") var stateManagement: String = null
  @Property("state-full-checkpoint") var stateFullCheckpoint: Int = 0
  @Property("event-wait-time") var eventWaitTime: Long = 0
  @Property("time-windowed") var timeWindowed: Int = 0
  @Property("window-full-max") var windowFullMax: Int = 0


  override def toProtocolInstance(): InstanceMetadata = {
    val protocolInstance = new WindowedInstanceMetadata()
    super.fillProtocolInstance(protocolInstance)

    protocolInstance.executionPlan = getProtocolExecutionPlan(this.executionPlan)
    protocolInstance.timeWindowed = this.timeWindowed
    protocolInstance.windowFullMax = this.windowFullMax
    protocolInstance.stateManagement = this.stateManagement
    protocolInstance.stateFullCheckpoint = this.stateFullCheckpoint
    protocolInstance.eventWaitTime = this.eventWaitTime
    protocolInstance.inputs = this.inputs
    protocolInstance.outputs = this.outputs
    protocolInstance.startFrom = this.startFrom

    protocolInstance
  }
}
