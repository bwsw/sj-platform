package com.bwsw.sj.common.DAL.model.module

import java.util

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.ZKService
import com.bwsw.sj.common.rest.entities.module.InstanceMetadata
import com.bwsw.sj.common.utils.EngineLiterals
import org.mongodb.morphia.annotations.{Embedded, Entity, Id, Property}

import scala.collection.JavaConverters._
/**
 * Entity for base instance-json
 *
 *
 * @author Kseniya Tomskikh
 */
@Entity("instances")
class Instance {
  @Property("module-type") var moduleType: String = null
  @Property("module-name") var moduleName: String = null
  @Property("module-version") var moduleVersion: String = null
  var status: String = EngineLiterals.ready
  @Property("rest-address") var restAddress: String = ""
  @Id var name: String = null
  var description: String = "No description"
  var outputs: Array[String] = Array()
  var parallelism: Int = 1
  var options: String = "{}"
  @Property("per-task-cores") var perTaskCores: Double = 0.1
  @Property("per-task-ram") var perTaskRam: Int = 32
  @Embedded("jvm-options") var jvmOptions: java.util.Map[String, String] = new util.HashMap[String, String]()
  @Property("node-attributes") var nodeAttributes: java.util.Map[String, String] = new util.HashMap[String, String]()
  @Embedded("coordination-service") var coordinationService: ZKService = null
  @Property("environment-variables") var environmentVariables: java.util.Map[String, String] = new util.HashMap[String, String]()
  var stage: FrameworkStage = new FrameworkStage()
  @Property("performance-reporting-interval") var performanceReportingInterval: Long = 60000
  var engine: String = null
  @Property("framework-id") val frameworkId: String = System.currentTimeMillis().toString

  def asProtocolInstance(): InstanceMetadata = ???

  protected def fillProtocolInstance(protocolInstance: InstanceMetadata) = {
    val serializer = new JsonSerializer()

    protocolInstance.status = this.status
    protocolInstance.name = this.name
    protocolInstance.description = this.description
    protocolInstance.parallelism = this.parallelism
    protocolInstance.options = serializer.deserialize[Map[String, Any]](this.options)
    protocolInstance.perTaskCores = this.perTaskCores
    protocolInstance.performanceReportingInterval = this.performanceReportingInterval
    protocolInstance.engine = this.engine
    protocolInstance.perTaskRam = this.perTaskRam
    protocolInstance.jvmOptions = Map(this.jvmOptions.asScala.toList: _*)
    protocolInstance.nodeAttributes = Map(this.nodeAttributes.asScala.toList: _*)
    protocolInstance.environmentVariables = Map(this.environmentVariables.asScala.toList: _*)
    protocolInstance.coordinationService = this.coordinationService.name
    protocolInstance.stage = this.stage
    protocolInstance.restAddress = this.restAddress
  }

  def getOptionsAsMap() = {
    val serializer = new JsonSerializer()
    serializer.deserialize[Map[String, Any]](this.options)
  }

  def getInputsWithoutStreamMode(): Array[String] = Array()
}
