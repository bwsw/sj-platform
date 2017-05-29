package com.bwsw.sj.common.dal.model.instance

import java.util

import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.PropertyField
import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals}
import org.mongodb.morphia.annotations.Property

/**
  * Domain entity for [[EngineLiterals.inputStreamingType]] instance
  *
  * @author Kseniya Tomskikh
  */
class InputInstanceDomain(override val name: String,
                          override val moduleType: String,
                          override val moduleName: String,
                          override val moduleVersion: String,
                          override val engine: String,
                          override val coordinationService: ZKServiceDomain,
                          override val status: String = EngineLiterals.ready,
                          override val restAddress: String = "",
                          override val description: String = RestLiterals.defaultDescription,
                          override val parallelism: Int = 1,
                          override val options: String = "{}",
                          override val perTaskCores: Double = 0.1,
                          override val perTaskRam: Int = 32,
                          override val jvmOptions: java.util.Map[String, String] = new util.HashMap[String, String](),
                          override val nodeAttributes: java.util.Map[String, String] = new util.HashMap[String, String](),
                          override val environmentVariables: java.util.Map[String, String] = new util.HashMap[String, String](),
                          override val stage: FrameworkStage = FrameworkStage(),
                          override val performanceReportingInterval: Long = 60000,
                          override val frameworkId: String = System.currentTimeMillis().toString,
                          override val outputs: Array[String] = Array(),
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
    frameworkId)
