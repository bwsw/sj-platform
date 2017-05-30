package com.bwsw.sj.crud.rest.model.instance.response

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.instance.FrameworkStage
import com.bwsw.sj.common.si.model.instance._
import com.bwsw.sj.common.utils.EngineLiterals

class InstanceApiResponse(val moduleName: String,
                          val moduleVersion: String,
                          val moduleType: String,
                          val stage: FrameworkStage,
                          val status: String,
                          val name: String,
                          val description: String,
                          val parallelism: Any,
                          val options: Map[String, Any],
                          val perTaskCores: Double,
                          val perTaskRam: Int,
                          val jvmOptions: Map[String, String],
                          val nodeAttributes: Map[String, String],
                          val coordinationService: String,
                          val environmentVariables: Map[String, String],
                          val performanceReportingInterval: Long,
                          val engine: String,
                          val restAddress: String)

object InstanceApiResponse {
  def from(instance: Instance): InstanceApiResponse = {
    val serializer = new JsonSerializer()
    instance.moduleType match {
      case EngineLiterals.inputStreamingType =>
        val inputInstance = instance.asInstanceOf[InputInstance]

        new InputInstanceApiResponse(
          inputInstance.moduleName,
          inputInstance.moduleVersion,
          inputInstance.moduleType,
          inputInstance.stage,
          inputInstance.status,
          inputInstance.name,
          inputInstance.description,
          inputInstance.parallelism,
          serializer.deserialize[Map[String, Any]](inputInstance.options),
          inputInstance.perTaskCores,
          inputInstance.perTaskRam,
          inputInstance.jvmOptions,
          inputInstance.nodeAttributes,
          inputInstance.coordinationService,
          inputInstance.environmentVariables,
          inputInstance.performanceReportingInterval,
          inputInstance.engine,
          inputInstance.restAddress.getOrElse(""),
          inputInstance.outputs,
          inputInstance.checkpointMode,
          inputInstance.checkpointInterval,
          inputInstance.duplicateCheck,
          inputInstance.lookupHistory,
          inputInstance.queueMaxSize,
          inputInstance.defaultEvictionPolicy,
          inputInstance.evictionPolicy,
          inputInstance.backupCount,
          inputInstance.asyncBackupCount,
          inputInstance.tasks.toMap)

      case EngineLiterals.regularStreamingType =>
        val regularInstance = instance.asInstanceOf[RegularInstance]

        new RegularInstanceApiResponse(
          regularInstance.moduleName,
          regularInstance.moduleVersion,
          regularInstance.moduleType,
          regularInstance.stage,
          regularInstance.status,
          regularInstance.name,
          regularInstance.description,
          regularInstance.parallelism,
          serializer.deserialize[Map[String, Any]](regularInstance.options),
          regularInstance.perTaskCores,
          regularInstance.perTaskRam,
          regularInstance.jvmOptions,
          regularInstance.nodeAttributes,
          regularInstance.coordinationService,
          regularInstance.environmentVariables,
          regularInstance.performanceReportingInterval,
          regularInstance.engine,
          regularInstance.restAddress.getOrElse(""),
          regularInstance.inputs,
          regularInstance.outputs,
          regularInstance.checkpointMode,
          regularInstance.checkpointInterval,
          regularInstance.executionPlan,
          regularInstance.startFrom,
          regularInstance.stateManagement,
          regularInstance.stateFullCheckpoint,
          regularInstance.eventWaitIdleTime,
          serializer.deserialize[Map[String, Any]](regularInstance.inputAvroSchema))

      case EngineLiterals.batchStreamingType =>
        val batchInstance = instance.asInstanceOf[BatchInstance]

        new BatchInstanceApiResponse(
          batchInstance.moduleName,
          batchInstance.moduleVersion,
          batchInstance.moduleType,
          batchInstance.stage,
          batchInstance.status,
          batchInstance.name,
          batchInstance.description,
          batchInstance.parallelism,
          serializer.deserialize[Map[String, Any]](batchInstance.options),
          batchInstance.perTaskCores,
          batchInstance.perTaskRam,
          batchInstance.jvmOptions,
          batchInstance.nodeAttributes,
          batchInstance.coordinationService,
          batchInstance.environmentVariables,
          batchInstance.performanceReportingInterval,
          batchInstance.engine,
          batchInstance.restAddress.getOrElse(""),
          batchInstance.inputs,
          batchInstance.window,
          batchInstance.slidingInterval,
          batchInstance.outputs,
          batchInstance.executionPlan,
          batchInstance.startFrom,
          batchInstance.stateManagement,
          batchInstance.stateFullCheckpoint,
          batchInstance.eventWaitIdleTime,
          serializer.deserialize[Map[String, Any]](batchInstance.inputAvroSchema))

      case EngineLiterals.outputStreamingType =>
        val outputInstance = instance.asInstanceOf[OutputInstance]

        new OutputInstanceApiResponse(
          outputInstance.moduleName,
          outputInstance.moduleVersion,
          outputInstance.moduleType,
          outputInstance.stage,
          outputInstance.status,
          outputInstance.name,
          outputInstance.description,
          outputInstance.parallelism,
          serializer.deserialize[Map[String, Any]](outputInstance.options),
          outputInstance.perTaskCores,
          outputInstance.perTaskRam,
          outputInstance.jvmOptions,
          outputInstance.nodeAttributes,
          outputInstance.coordinationService,
          outputInstance.environmentVariables,
          outputInstance.performanceReportingInterval,
          outputInstance.engine,
          outputInstance.restAddress.getOrElse(""),
          outputInstance.checkpointMode,
          outputInstance.checkpointInterval,
          outputInstance.executionPlan,
          outputInstance.startFrom,
          outputInstance.input,
          outputInstance.output,
          serializer.deserialize[Map[String, Any]](outputInstance.inputAvroSchema))
    }
  }
}
