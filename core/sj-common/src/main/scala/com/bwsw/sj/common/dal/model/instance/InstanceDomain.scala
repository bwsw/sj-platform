package com.bwsw.sj.common.dal.model.instance

import java.util

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.{EmbeddedField, IdField, PropertyField}
import com.bwsw.sj.common.rest.model.module.InstanceApi
import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals}
import org.mongodb.morphia.annotations.Entity

import scala.collection.JavaConverters._

/**
  * Domain entity for instance
  *
  * @author Kseniya Tomskikh
  */
@Entity("instances")
class InstanceDomain(@IdField val name: String,
                     @PropertyField("module-type") val moduleType: String,
                     @PropertyField("module-name") val moduleName: String,
                     @PropertyField("module-version") val moduleVersion: String,
                     val engine: String,
                     @EmbeddedField("coordination-service") val coordinationService: ZKServiceDomain,
                     var status: String = EngineLiterals.ready,
                     @PropertyField("rest-address") var restAddress: String = "",
                     var description: String = RestLiterals.defaultDescription,
                     var outputs: Array[String] = Array(),
                     var parallelism: Int = 1,
                     var options: String = "{}",
                     @PropertyField("per-task-cores") var perTaskCores: Double = 0.1,
                     @PropertyField("per-task-ram") var perTaskRam: Int = 32,
                     @PropertyField("jvm-options") var jvmOptions: java.util.Map[String, String] = new util.HashMap[String, String](),
                     @PropertyField("node-attributes") var nodeAttributes: java.util.Map[String, String] = new util.HashMap[String, String](),
                     @PropertyField("environment-variables") var environmentVariables: java.util.Map[String, String] = new util.HashMap[String, String](),
                     var stage: FrameworkStage = FrameworkStage(),
                     @PropertyField("performance-reporting-interval") var performanceReportingInterval: Long = 60000,
                     @PropertyField("framework-id") val frameworkId: String = System.currentTimeMillis().toString
              ) {

  def asProtocolInstance(): InstanceApi = ???

  protected def fillProtocolInstance(protocolInstance: InstanceApi): Unit = {
    val serializer = new JsonSerializer()

    protocolInstance.status = this.status
    protocolInstance.name = this.name
    protocolInstance.moduleName = this.moduleName
    protocolInstance.moduleType = this.moduleType
    protocolInstance.moduleVersion = this.moduleVersion
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

  def getInputsWithoutStreamMode(): Array[String] = Array()
}
