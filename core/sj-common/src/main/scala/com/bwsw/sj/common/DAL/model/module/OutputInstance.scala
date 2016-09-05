package com.bwsw.sj.common.DAL.model.module

import com.bwsw.sj.common.rest.entities.module.{InstanceMetadata, OutputInstanceMetadata}
import org.mongodb.morphia.annotations.Property

/**
 * Entity for output-streaming instance-json
 *
 *
 * @author Kseniya Tomskikh
 */
class OutputInstance() extends Instance {
  @Property("start-from") var startFrom: String = null

  override def toProtocolInstance(): InstanceMetadata = {
    val protocolInstance = new OutputInstanceMetadata()
    super.fillProtocolInstance(protocolInstance)

    protocolInstance.executionPlan = getProtocolExecutionPlan()
    protocolInstance.input = this.inputs.head
    protocolInstance.output = this.outputs.head
    protocolInstance.startFrom = this.startFrom

    protocolInstance
  }
}
