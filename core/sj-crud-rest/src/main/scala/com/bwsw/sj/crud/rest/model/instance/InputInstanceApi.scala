package com.bwsw.sj.crud.rest.model.instance

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.si.model.instance.InputInstance
import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

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
                       @JsonDeserialize(contentAs = classOf[Double]) perTaskCores: Option[Double] = Some(1),
                       @JsonDeserialize(contentAs = classOf[Int]) perTaskRam: Option[Int] = Some(1024),
                       jvmOptions: Map[String, String] = Map(),
                       nodeAttributes: Map[String, String] = Map(),
                       environmentVariables: Map[String, String] = Map(),
                       @JsonDeserialize(contentAs = classOf[Long]) performanceReportingInterval: Option[Long] = Some(60000),
                       @JsonDeserialize(contentAs = classOf[Boolean]) val duplicateCheck: Option[Boolean] = Some(false),
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

  override def to(moduleType: String, moduleName: String, moduleVersion: String): InputInstance = {
    val serializer = new JsonSerializer()

    new InputInstance(
      name,
      Option(description).getOrElse(RestLiterals.defaultDescription),
      Option(parallelism).getOrElse(1),
      serializer.serialize(Option(options).getOrElse(Map())),
      perTaskCores.getOrElse(1),
      perTaskRam.getOrElse(1024),
      Option(jvmOptions).getOrElse(Map()),
      Option(nodeAttributes).getOrElse(Map()),
      coordinationService,
      Option(environmentVariables).getOrElse(Map()),
      performanceReportingInterval.getOrElse(60000l),
      moduleName,
      moduleVersion,
      moduleType,
      getEngine(moduleType, moduleName, moduleVersion),
      checkpointMode,
      checkpointInterval,
      outputs,
      lookupHistory,
      queueMaxSize,
      duplicateCheck.getOrElse(false),
      Option(defaultEvictionPolicy).getOrElse(EngineLiterals.noneDefaultEvictionPolicy),
      Option(evictionPolicy).getOrElse(EngineLiterals.fixTimeEvictionPolicy),
      backupCount,
      asyncBackupCount)
  }
}


