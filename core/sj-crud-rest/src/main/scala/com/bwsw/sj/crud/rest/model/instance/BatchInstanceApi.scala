package com.bwsw.sj.crud.rest.model.instance

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.si.model.instance.BatchInstance
import com.bwsw.sj.common.utils.{AvroRecordUtils, EngineLiterals, RestLiterals}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import scaldi.Injector

/**
  * API entity for [[EngineLiterals.batchStreamingType]] instance
  */
class BatchInstanceApi(name: String,
                       coordinationService: String,
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
                       @JsonDeserialize(contentAs = classOf[Int]) val window: Option[Int] = Some(1),
                       @JsonDeserialize(contentAs = classOf[Int]) val slidingInterval: Option[Int] = Some(1),
                       val startFrom: String = EngineLiterals.newestStartMode,
                       val stateManagement: String = EngineLiterals.noneStateMode,
                       @JsonDeserialize(contentAs = classOf[Int]) val stateFullCheckpoint: Option[Int] = Some(100),
                       @JsonDeserialize(contentAs = classOf[Long]) val eventWaitTime: Option[Long] = Some(1000),
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

  override def to(moduleType: String, moduleName: String, moduleVersion: String)
                 (implicit injector: Injector): BatchInstance = {
    val serializer = new JsonSerializer()

    new BatchInstance(
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
      inputs,
      outputs,
      window.getOrElse(1),
      slidingInterval.getOrElse(1),
      Option(startFrom).getOrElse(EngineLiterals.newestStartMode),
      Option(stateManagement).getOrElse(EngineLiterals.noneStateMode),
      stateFullCheckpoint.getOrElse(100),
      eventWaitTime.getOrElse(1000l),
      AvroRecordUtils.mapToSchema(Option(inputAvroSchema).getOrElse(Map())))
  }
}
