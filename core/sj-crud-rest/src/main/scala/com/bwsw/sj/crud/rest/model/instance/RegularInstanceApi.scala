package com.bwsw.sj.crud.rest.model.instance

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.si.model.instance.RegularInstance
import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

/**
  * API entity for [[EngineLiterals.regularStreamingType]] instance
  */
class RegularInstanceApi(name: String,
                         coordinationService: String,
                         val checkpointMode: String,
                         @JsonProperty(required = true) val checkpointInterval: Long,
                         val inputs: Array[String],
                         val outputs: Array[String],
                         description: String = RestLiterals.defaultDescription,
                         parallelism: Any = 1,
                         options: Map[String, Any] = Map(),
                         @JsonDeserialize(contentAs = classOf[Double]) perTaskCores: Option[Double] = Some(1),
                         @JsonDeserialize(contentAs = classOf[Int]) perTaskRam: Option[Int] = Some(1024),
                         jvmOptions: Map[String, String] = Map(),
                         nodeAttributes: Map[String, String] = Map(),
                         environmentVariables: Map[String, String] = Map(),
                         @JsonDeserialize(contentAs = classOf[Long]) performanceReportingInterval: Option[Long] = Some(60000),
                         val startFrom: String = EngineLiterals.newestStartMode,
                         val stateManagement: String = EngineLiterals.noneStateMode,
                         @JsonDeserialize(contentAs = classOf[Int]) val stateFullCheckpoint: Option[Int] = Some(100),
                         @JsonDeserialize(contentAs = classOf[Long]) val eventWaitIdleTime: Option[Long] = Some(1000),
                         val inputAvroSchema: Map[String, Any] = Map())
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

  override def to(moduleType: String, moduleName: String, moduleVersion: String): RegularInstance = {
    val serializer = new JsonSerializer()

    new RegularInstance(
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
      Option(inputs).getOrElse(Array()),
      Option(outputs).getOrElse(Array()),
      checkpointMode,
      checkpointInterval,
      Option(startFrom).getOrElse(EngineLiterals.newestStartMode),
      Option(stateManagement).getOrElse(EngineLiterals.noneStateMode),
      stateFullCheckpoint.getOrElse(100),
      eventWaitIdleTime.getOrElse(1000l),
      serializer.serialize(Option(inputAvroSchema).getOrElse(Map())))
  }
}
