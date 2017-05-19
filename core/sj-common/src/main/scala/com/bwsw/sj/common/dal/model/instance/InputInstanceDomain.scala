package com.bwsw.sj.common.dal.model.instance

import java.util

import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.PropertyField
import com.bwsw.sj.common.rest.model.module.{InputInstanceApi, InstanceApi}
import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals}
import org.mongodb.morphia.annotations.Property

import scala.collection.JavaConverters._

/**
  * Entity for input instance-json
  *
  * @author Kseniya Tomskikh
  */
class InputInstanceDomain(override val name: String,
                          override val moduleType: String,
                          override val moduleName: String,
                          override val moduleVersion: String,
                          override val engine: String,
                          override val coordinationService: ZKServiceDomain,
                          status: String = EngineLiterals.ready,
                          restAddress: String = "",
                          description: String = RestLiterals.defaultDescription,
                          parallelism: Int = 1,
                          options: String = "{}",
                          perTaskCores: Double = 0.1,
                          perTaskRam: Int = 32,
                          jvmOptions: java.util.Map[String, String] = new util.HashMap[String, String](),
                          nodeAttributes: java.util.Map[String, String] = new util.HashMap[String, String](),
                          environmentVariables: java.util.Map[String, String] = new util.HashMap[String, String](),
                          stage: FrameworkStage = FrameworkStage(),
                          performanceReportingInterval: Long = 60000,
                          override val frameworkId: String = System.currentTimeMillis().toString,
                          outputs: Array[String] = Array(),
                          @PropertyField("checkpoint-mode") val checkpointMode: String,
                          @Property("checkpoint-interval") var checkpointInterval: Long = 0,
                          @Property("duplicate-check") var duplicateCheck: Boolean = false,
                          @Property("lookup-history") var lookupHistory: Int = 0,
                          @Property("queue-max-size") var queueMaxSize: Int = 0,
                          @Property("default-eviction-policy") var defaultEvictionPolicy: String = EngineLiterals.noneDefaultEvictionPolicy,
                          @Property("eviction-policy") var evictionPolicy: String = EngineLiterals.fixTimeEvictionPolicy,
                          @Property("backup-count") var backupCount: Int = 0,
                          @Property("async-backup-count") var asyncBackupCount: Int = 0,
                          var tasks: java.util.Map[String, InputTask] = new util.HashMap())
  extends InstanceDomain(
    name,
    moduleType,
    moduleName,
    moduleVersion,
    engine,
    coordinationService,
    status,
    restAddress,
    description,
    outputs,
    parallelism,
    options,
    perTaskCores,
    perTaskRam,
    jvmOptions,
    nodeAttributes,
    environmentVariables,
    stage,
    performanceReportingInterval,
    frameworkId) {

  override def asProtocolInstance(): InstanceApi = {
    val protocolInstance = new InputInstanceApi()
    super.fillProtocolInstance(protocolInstance)

    protocolInstance.outputs = this.outputs
    protocolInstance.checkpointMode = this.checkpointMode
    protocolInstance.checkpointInterval = this.checkpointInterval
    protocolInstance.duplicateCheck = this.duplicateCheck
    protocolInstance.lookupHistory = this.lookupHistory
    protocolInstance.queueMaxSize = this.queueMaxSize
    protocolInstance.defaultEvictionPolicy = this.defaultEvictionPolicy
    protocolInstance.evictionPolicy = this.evictionPolicy
    protocolInstance.backupCount = this.backupCount
    protocolInstance.asyncBackupCount = this.asyncBackupCount
    protocolInstance.tasks = Map(this.tasks.asScala.toList: _*)

    protocolInstance
  }
}
