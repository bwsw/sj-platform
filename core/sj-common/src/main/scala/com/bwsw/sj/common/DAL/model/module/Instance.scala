package com.bwsw.sj.common.DAL.model.module

import java.util

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.ZKService
import com.bwsw.sj.common.rest.entities.module.InstanceMetadata
import org.mongodb.morphia.annotations.{Embedded, Entity, Id, Property}
import scala.collection.JavaConversions._
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
  var status: String = null
  @Id var name: String = null
  var description: String = "No description"
  var inputs: Array[String] = Array()
  var outputs: Array[String] = Array()
  @Property("checkpoint-mode") var checkpointMode: String = null
  @Property("checkpoint-interval") var checkpointInterval: Long = 0
  var parallelism: Int = 0
  var options: String = null
  @Property("per-task-cores") var perTaskCores: Double = 0.0
  @Property("per-task-ram") var perTaskRam: Int = 0
  @Embedded("jvm-options") var jvmOptions: java.util.Map[String, String] = new util.HashMap[String, String]()
  @Property("node-attributes") var nodeAttributes: java.util.Map[String, String] = new util.HashMap[String, String]()
  @Embedded("coordination-service") var coordinationService: ZKService = null
  @Property("environment-variables") var environmentVariables: java.util.Map[String, String] = new util.HashMap[String, String]()
  var stages: java.util.Map[String, InstanceStage] = new util.HashMap()
  @Property("performance-reporting-interval") var performanceReportingInterval: Long = 0
  var engine: String = null

  def asProtocolInstance(): InstanceMetadata = ???

  protected def fillProtocolInstance(protocolInstance: InstanceMetadata) = {
    val serializer = new JsonSerializer()

    protocolInstance.status = this.status
    protocolInstance.name = this.name
    protocolInstance.description = this.description
    protocolInstance.checkpointMode = this.checkpointMode
    protocolInstance.checkpointInterval = this.checkpointInterval
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
  }

  protected def getProtocolExecutionPlan(executionPlan: ExecutionPlan) = {
    val protocolExecutionPlan = Map(
      "tasks" -> executionPlan.tasks.map(t => t._1 -> Map("inputs" -> t._2.inputs))
    )

    protocolExecutionPlan
  }
}
