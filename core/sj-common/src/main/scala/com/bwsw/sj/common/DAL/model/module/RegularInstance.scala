package com.bwsw.sj.common.DAL.model.module

import com.bwsw.sj.common.rest.entities.module.{InstanceMetadata, RegularInstanceMetadata}
import com.bwsw.sj.common.utils.EngineLiterals
import org.mongodb.morphia.annotations._

/**
 * Entity for regular instance-json
 *
 * @author Kseniya Tomskikh
 */
class RegularInstance() extends Instance {
  @Embedded("execution-plan") var executionPlan: ExecutionPlan = null
  @Property("start-from") var startFrom: String = EngineLiterals.newestStartMode
  @Property("state-management") var stateManagement: String = EngineLiterals.noneStateMode
  @Property("state-full-checkpoint") var stateFullCheckpoint: Int = 100
  @Property("event-wait-time") var eventWaitTime: Long = 1000

  override def asProtocolInstance(): InstanceMetadata = {
    val protocolInstance = new RegularInstanceMetadata()
    super.fillProtocolInstance(protocolInstance)

    protocolInstance.executionPlan = this.executionPlan
    protocolInstance.stateManagement = this.stateManagement
    protocolInstance.stateFullCheckpoint = this.stateFullCheckpoint
    protocolInstance.eventWaitTime = this.eventWaitTime
    protocolInstance.inputs = this.inputs
    protocolInstance.outputs = this.outputs
    protocolInstance.startFrom = this.startFrom

    protocolInstance
  }
}





