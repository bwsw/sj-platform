package com.bwsw.sj.common.rest.entities.module

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.ZKService
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.fasterxml.jackson.annotation.JsonProperty
import scala.collection.JavaConversions._

class InstanceMetadata {
  var status: String = null
  var name: String = null
  var description: String = "No description"
  @JsonProperty("checkpoint-mode") var checkpointMode: String = null
  @JsonProperty("checkpoint-interval") var checkpointInterval: Long = 0L
  var parallelism: Any = 1
  var options: Map[String, Any] = Map()
  @JsonProperty("per-task-cores") var perTaskCores: Double = 1.0
  @JsonProperty("per-task-ram") var perTaskRam: Int = 1024
  @JsonProperty("jvm-options") var jvmOptions: Map[String, String] = Map()
  @JsonProperty("node-attributes") var nodeAttributes: Map[String, String] = Map()
  @JsonProperty("coordination-service") var coordinationService: String = null
  @JsonProperty("environment-variables") var environmentVariables: Map[String, String] = Map()
  @JsonProperty("performance-reporting-interval") var performanceReportingInterval: Long = 60000
  var engine: String = null

  def asModelInstance(): Instance = ???

  protected def fillModelInstance(modelInstance: Instance) = {
    val serializer = new JsonSerializer()
    val serviceDAO = ConnectionRepository.getServiceManager

    modelInstance.name = this.name
    modelInstance.description = this.description
    modelInstance.checkpointMode = this.checkpointMode
    modelInstance.checkpointInterval = this.checkpointInterval
    modelInstance.parallelism = this.parallelism.asInstanceOf[Int]
    modelInstance.perTaskCores = this.perTaskCores
    modelInstance.perTaskRam = this.perTaskRam
    modelInstance.performanceReportingInterval = this.performanceReportingInterval
    modelInstance.engine = this.engine
    modelInstance.options = serializer.serialize(this.options)
    modelInstance.jvmOptions = mapAsJavaMap(this.jvmOptions)
    modelInstance.nodeAttributes = mapAsJavaMap(this.nodeAttributes)
    modelInstance.environmentVariables = mapAsJavaMap(this.environmentVariables)
    val service = serviceDAO.get(this.coordinationService)
    if (service.isDefined && service.get.isInstanceOf[ZKService]) {
      modelInstance.coordinationService = service.get.asInstanceOf[ZKService]
    }
  }
}

