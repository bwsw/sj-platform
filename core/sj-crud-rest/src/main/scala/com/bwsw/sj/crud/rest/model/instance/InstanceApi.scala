package com.bwsw.sj.crud.rest.model.instance

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.instance._
import com.bwsw.sj.common.si.model.module.Specification
import com.bwsw.sj.common.utils.{AvroUtils, EngineLiterals, RestLiterals}

class InstanceApi(val name: String,
                  val coordinationService: String,
                  val description: String = RestLiterals.defaultDescription,
                  val parallelism: Any = 1,
                  val options: Map[String, Any] = Map(),
                  val perTaskCores: Double = 1,
                  val perTaskRam: Int = 1024,
                  val jvmOptions: Map[String, String] = Map(),
                  val nodeAttributes: Map[String, String] = Map(),
                  val environmentVariables: Map[String, String] = Map(),
                  val performanceReportingInterval: Long = 60000) {


  def to(moduleType: String, moduleName: String, moduleVersion: String): Instance = {
    new Instance(
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
      getEngine(moduleType, moduleName, moduleVersion))
  }


  protected def getFilesMetadata(moduleType: String, moduleName: String, moduleVersion: String) = {
    val fileMetadataDAO = ConnectionRepository.getFileMetadataRepository
    fileMetadataDAO.getByParameters(Map("filetype" -> "module",
      "specification.name" -> moduleName,
      "specification.module-type" -> moduleType,
      "specification.version" -> moduleVersion)
    )
  }

  protected def getEngine(moduleType: String, moduleName: String, moduleVersion: String): String = {
    val filesMetadata = getFilesMetadata(moduleType, moduleName, moduleVersion)
    val fileMetadata = filesMetadata.head
    val specification = Specification.from(fileMetadata.specification)
    specification.engineName + "-" + specification.engineVersion
  }
}

object InstanceApi {

  def from(instance: Instance): InstanceApi = instance.moduleType match {
    case EngineLiterals.inputStreamingType =>
      val inputInstance = instance.asInstanceOf[InputInstance]

      new InputInstanceApi(
        inputInstance.name,
        inputInstance.coordinationService,
        inputInstance.checkpointMode,
        inputInstance.checkpointInterval,
        inputInstance.outputs,
        inputInstance.lookupHistory,
        inputInstance.queueMaxSize,
        inputInstance.description,
        inputInstance.parallelism,
        inputInstance.options,
        inputInstance.perTaskCores,
        inputInstance.perTaskRam,
        inputInstance.jvmOptions,
        inputInstance.nodeAttributes,
        inputInstance.environmentVariables,
        inputInstance.performanceReportingInterval,
        inputInstance.duplicateCheck,
        inputInstance.defaultEvictionPolicy,
        inputInstance.evictionPolicy,
        inputInstance.backupCount,
        inputInstance.asyncBackupCount)

    case EngineLiterals.regularStreamingType =>
      val regularInstance = instance.asInstanceOf[RegularInstance]

      new RegularInstanceApi(
        regularInstance.name,
        regularInstance.coordinationService,
        regularInstance.checkpointMode,
        regularInstance.checkpointInterval,
        regularInstance.inputs,
        regularInstance.outputs,
        regularInstance.description,
        regularInstance.parallelism,
        regularInstance.options,
        regularInstance.perTaskCores,
        regularInstance.perTaskRam,
        regularInstance.jvmOptions,
        regularInstance.nodeAttributes,
        regularInstance.environmentVariables,
        regularInstance.performanceReportingInterval,
        regularInstance.startFrom,
        regularInstance.stateManagement,
        regularInstance.stateFullCheckpoint,
        regularInstance.eventWaitIdleTime,
        AvroUtils.schemaToMap(regularInstance.inputAvroSchema))

    case EngineLiterals.batchStreamingType =>
      val batchInstance = instance.asInstanceOf[BatchInstance]

      new BatchInstanceApi(
        batchInstance.name,
        batchInstance.coordinationService,
        batchInstance.inputs,
        batchInstance.outputs,
        batchInstance.description,
        batchInstance.parallelism,
        batchInstance.options,
        batchInstance.perTaskCores,
        batchInstance.perTaskRam,
        batchInstance.jvmOptions,
        batchInstance.nodeAttributes,
        batchInstance.environmentVariables,
        batchInstance.performanceReportingInterval,
        batchInstance.window,
        batchInstance.slidingInterval,
        batchInstance.startFrom,
        batchInstance.stateManagement,
        batchInstance.stateFullCheckpoint,
        batchInstance.eventWaitTime,
        AvroUtils.schemaToMap(batchInstance.inputAvroSchema))

    case EngineLiterals.outputStreamingType =>
      val regularInstance = instance.asInstanceOf[OutputInstance]

      new OutputInstanceApi(
        regularInstance.name,
        regularInstance.coordinationService,
        regularInstance.checkpointMode,
        regularInstance.checkpointInterval,
        regularInstance.input,
        regularInstance.output,
        regularInstance.description,
        regularInstance.parallelism,
        regularInstance.options,
        regularInstance.perTaskCores,
        regularInstance.perTaskRam,
        regularInstance.jvmOptions,
        regularInstance.nodeAttributes,
        regularInstance.environmentVariables,
        regularInstance.performanceReportingInterval,
        regularInstance.startFrom,
        AvroUtils.schemaToMap(regularInstance.inputAvroSchema))

    case _ =>
      new InstanceApi(
        instance.name,
        instance.coordinationService,
        instance.description,
        instance.parallelism,
        instance.options,
        instance.perTaskCores,
        instance.perTaskRam,
        instance.jvmOptions,
        instance.nodeAttributes,
        instance.environmentVariables,
        instance.performanceReportingInterval)
  }
}
