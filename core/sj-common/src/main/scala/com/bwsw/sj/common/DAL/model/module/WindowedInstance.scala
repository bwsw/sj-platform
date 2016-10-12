package com.bwsw.sj.common.DAL.model.module

import com.bwsw.sj.common.rest.entities.module.{BatchFillType, ExecutionPlan, InstanceMetadata, WindowedInstanceMetadata}
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.SjStreamUtils._
import com.fasterxml.jackson.annotation.JsonProperty
import org.mongodb.morphia.annotations.{Embedded, Property}

/**
 * Entity for windowed instance-json
 *
 * @author Kseniya Tomskikh
 */
class WindowedInstance() extends Instance {
  @JsonProperty("main-stream") var mainStream: String = null
  @JsonProperty("related-streams") var relatedStreams: Array[String] = Array()
  @JsonProperty("batch-fill-type") var batchFillType: BatchFillType = null
  var window: Int = 1
  @JsonProperty("sliding-interval") var slidingInterval: Int = 1
  @Embedded("execution-plan") var executionPlan: ExecutionPlan = new ExecutionPlan()
  @Property("start-from") var startFrom: String = EngineLiterals.newestStartMode
  @Property("state-management") var stateManagement: String = EngineLiterals.noneStateMode
  @Property("state-full-checkpoint") var stateFullCheckpoint: Int = 100
  @Property("event-wait-time") var eventWaitTime: Long = 1000

  override def asProtocolInstance(): InstanceMetadata = {
    val protocolInstance = new WindowedInstanceMetadata()
    super.fillProtocolInstance(protocolInstance)

    protocolInstance.mainStream = this.mainStream
    protocolInstance.relatedStreams = this.relatedStreams
    protocolInstance.batchFillType = this.batchFillType
    protocolInstance.window = this.window
    protocolInstance.slidingInterval = this.slidingInterval
    protocolInstance.eventWaitTime = this.eventWaitTime
    protocolInstance.executionPlan = this.executionPlan
    protocolInstance.stateManagement = this.stateManagement
    protocolInstance.stateFullCheckpoint = this.stateFullCheckpoint
    protocolInstance.outputs = this.outputs
    protocolInstance.startFrom = this.startFrom

    protocolInstance
  }

  override def getInputsWithoutStreamMode() = (this.relatedStreams :+ this.mainStream).map(clearStreamFromMode)
}
