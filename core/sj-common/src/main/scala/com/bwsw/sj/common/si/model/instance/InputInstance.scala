package com.bwsw.sj.common.si.model.instance

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.instance.{FrameworkStage, InputInstanceDomain, InputTask}
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals

import scala.collection.JavaConverters._

class InputInstance(name: String,
                    description: String,
                    parallelism: Any,
                    options: Map[String, Any],
                    perTaskCores: Double,
                    perTaskRam: Int,
                    jvmOptions: Map[String, String],
                    nodeAttributes: Map[String, String],
                    coordinationService: String,
                    environmentVariables: Map[String, String],
                    performanceReportingInterval: Long,
                    moduleName: String,
                    moduleVersion: String,
                    moduleType: String,
                    engine: String,
                    val checkpointMode: String,
                    val checkpointInterval: Long,
                    val outputs: Array[String],
                    val lookupHistory: Int,
                    val queueMaxSize: Int,
                    val duplicateCheck: Boolean,
                    val defaultEvictionPolicy: String,
                    val evictionPolicy: String,
                    val backupCount: Int,
                    val asyncBackupCount: Int,
                    var tasks: Map[String, InputTask] = Map(),
                    restAddress: Option[String] = None,
                    stage: FrameworkStage = FrameworkStage(),
                    status: String = EngineLiterals.ready,
                    frameworkId: String = System.currentTimeMillis().toString)
  extends Instance(
    name,
    description,
    parallelism,
    options,
    perTaskCores,
    perTaskRam,
    jvmOptions,
    nodeAttributes,
    coordinationService,
    environmentVariables,
    performanceReportingInterval,
    moduleName,
    moduleVersion,
    moduleType,
    engine,
    restAddress,
    stage,
    status,
    frameworkId) {

  override def to: InputInstanceDomain = {
    val serializer = new JsonSerializer
    val serviceRepository = ConnectionRepository.getServiceRepository

    new InputInstanceDomain(
      name,
      moduleType,
      moduleName,
      moduleVersion,
      engine,
      serviceRepository.get(coordinationService).asInstanceOf[ZKServiceDomain],
      status,
      restAddress.getOrElse(""),
      description,
      countParallelism,
      serializer.serialize(options),
      perTaskCores,
      perTaskRam,
      jvmOptions.asJava,
      nodeAttributes.asJava,
      environmentVariables.asJava,
      stage,
      performanceReportingInterval,
      frameworkId,

      outputs,
      checkpointMode,
      checkpointInterval,
      duplicateCheck,
      lookupHistory,
      queueMaxSize,
      defaultEvictionPolicy,
      evictionPolicy,
      backupCount,
      asyncBackupCount,
      tasks.asJava)
  }
}
