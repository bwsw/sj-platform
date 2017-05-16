package com.bwsw.sj.crud.rest.model.instance

import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals}

class InputInstanceApi(name: String,
                       coordinationService: String,
                       val checkpointMode: String,
                       val checkpointInterval: Long,
                       val outputs: Array[String],
                       val lookupHistory: Int,
                       val queueMaxSize: Int,
                       description: String = RestLiterals.defaultDescription,
                       parallelism: Any = 1,
                       options: Map[String, Any] = Map(),
                       perTaskCores: Double = 1,
                       perTaskRam: Int = 1024,
                       jvmOptions: Map[String, String] = Map(),
                       nodeAttributes: Map[String, String] = Map(),
                       environmentVariables: Map[String, String] = Map(),
                       performanceReportingInterval: Long = 60000,
                       val duplicateCheck: Boolean = false,
                       val defaultEvictionPolicy: String = EngineLiterals.noneDefaultEvictionPolicy,
                       val evictionPolicy: String = EngineLiterals.fixTimeEvictionPolicy,
                       val backupCount: Int = 0,
                       val asyncBackupCount: Int = 0)
  extends InstanceApi(
    name,
    coordinationService,
    description,
    parallelism,
    options,
    perTaskCores,
    perTaskRam,
    jvmOptions,
    nodeAttributes,
    environmentVariables,
    performanceReportingInterval) {

}


