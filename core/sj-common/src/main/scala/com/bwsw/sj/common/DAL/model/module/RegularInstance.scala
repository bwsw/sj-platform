package com.bwsw.sj.common.DAL.model.module

import com.bwsw.sj.common.rest.entities.module.{InstanceMetadata, RegularInstanceMetadata}
import org.mongodb.morphia.annotations._

/**
 * Entity for regular instance-json
 *
 * @author Kseniya Tomskikh
 */
class RegularInstance() extends Instance {
  @Property("start-from") var startFrom: String = null
  @Property("state-management") var stateManagement: String = null
  @Property("state-full-checkpoint") var stateFullCheckpoint: Int = 0
  @Property("event-wait-time") var eventWaitTime: Long = 0

  override def toProtocolInstance(): InstanceMetadata = {
    val protocolInstance = new RegularInstanceMetadata()
    super.fillProtocolInstance(protocolInstance)

    protocolInstance.executionPlan = getProtocolExecutionPlan()
    protocolInstance.stateManagement = this.stateManagement
    protocolInstance.stateFullCheckpoint = this.stateFullCheckpoint
    protocolInstance.eventWaitTime = this.eventWaitTime
    protocolInstance.inputs = this.inputs
    protocolInstance.outputs = this.outputs
    protocolInstance.startFrom = this.startFrom

    protocolInstance
  }
}





