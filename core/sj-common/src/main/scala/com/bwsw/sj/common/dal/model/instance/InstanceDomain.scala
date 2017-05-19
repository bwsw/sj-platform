package com.bwsw.sj.common.dal.model.instance

import java.util

import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.{EmbeddedField, IdField, PropertyField}
import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals}
import org.mongodb.morphia.annotations.Entity

/**
  * Entity for base instance-json
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
                     @PropertyField("framework-id") val frameworkId: String = System.currentTimeMillis().toString) {

  def getInputsWithoutStreamMode(): Array[String] = Array()
}
