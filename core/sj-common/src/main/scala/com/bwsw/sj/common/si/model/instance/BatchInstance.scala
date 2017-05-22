package com.bwsw.sj.common.si.model.instance

import com.bwsw.sj.common.dal.model.instance.{BatchInstanceDomain, ExecutionPlan, FrameworkStage}
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.SjStreamUtils.clearStreamFromMode
import com.bwsw.sj.common.utils.{AvroUtils, EngineLiterals, RestLiterals}
import org.apache.avro.Schema

import scala.collection.JavaConverters._

class BatchInstance(name: String,
                    description: String = RestLiterals.defaultDescription,
                    parallelism: Any = 1,
                    options: String = "{}",
                    perTaskCores: Double = 1,
                    perTaskRam: Int = 1024,
                    jvmOptions: Map[String, String] = Map(),
                    nodeAttributes: Map[String, String] = Map(),
                    coordinationService: String,
                    environmentVariables: Map[String, String] = Map(),
                    performanceReportingInterval: Long = 60000,
                    moduleName: String,
                    moduleVersion: String,
                    moduleType: String,
                    engine: String,
                    val inputs: Array[String],
                    outputs: Array[String],
                    val window: Int = 1,
                    val slidingInterval: Int = 1,
                    val startFrom: String = EngineLiterals.newestStartMode,
                    val stateManagement: String = EngineLiterals.noneStateMode,
                    val stateFullCheckpoint: Int = 100,
                    val eventWaitTime: Long = 1000,
                    val inputAvroSchema: Option[Schema] = None,
                    val executionPlan: ExecutionPlan = new ExecutionPlan(),
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
    frameworkId,
    outputs) {

  override def to: BatchInstanceDomain = {
    val serviceRepository = ConnectionRepository.getServiceRepository

    new BatchInstanceDomain(
      name,
      moduleType,
      moduleName,
      moduleVersion,
      engine,
      serviceRepository.get(coordinationService).get.asInstanceOf[ZKServiceDomain],
      status,
      restAddress.getOrElse(""),
      description,
      countParallelism,
      options,
      perTaskCores,
      perTaskRam,
      jvmOptions.asJava,
      nodeAttributes.asJava,
      environmentVariables.asJava,
      stage,
      performanceReportingInterval,
      frameworkId,
      inputs,
      outputs,
      window,
      slidingInterval,
      executionPlan,
      startFrom,
      stateManagement,
      stateFullCheckpoint,
      eventWaitTime,
      AvroUtils.schemaToJson(inputAvroSchema))
  }

  override def inputsOrEmptyList: Array[String] = inputs

  override def prepareInstance(): Unit =
    executionPlan.fillTasks(createTaskStreams(), createTaskNames(countParallelism, name))

  override def countParallelism: Int =
    castParallelismToNumber(getStreamsPartitions(streams))

  override def createStreams(): Unit =
    getStreams(streams).foreach(_.create())

  override def getInputsWithoutStreamMode: Array[String] = inputs.map(clearStreamFromMode)

  override val streams = getInputsWithoutStreamMode ++ outputs
}
