package com.bwsw.sj.common.si.model.instance

import com.bwsw.sj.common.dal.model.instance.{ExecutionPlan, FrameworkStage, OutputInstanceDomain}
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.StreamUtils.clearStreamFromMode
import com.bwsw.sj.common.utils.{AvroRecordUtils, EngineLiterals, RestLiterals}
import org.apache.avro.Schema

import scala.collection.JavaConverters._

class OutputInstance(name: String,
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
                     val checkpointMode: String,
                     val checkpointInterval: Long,
                     val input: String,
                     val output: String,
                     val startFrom: String = EngineLiterals.newestStartMode,
                     val inputAvroSchema: Option[Schema] = None,
                     val executionPlan: ExecutionPlan = new ExecutionPlan(),
                     restAddress: Option[String] = None,
                     stage: FrameworkStage = FrameworkStage(),
                     private val _status: String = EngineLiterals.ready,
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
    _status,
    frameworkId,
    Array(output)) {

  override def to: OutputInstanceDomain = {
    val serviceRepository = ConnectionRepository.getServiceRepository

    new OutputInstanceDomain(
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
      Array(input),
      Array(output),
      checkpointMode,
      checkpointInterval,
      executionPlan,
      startFrom,
      AvroRecordUtils.schemaToJson(inputAvroSchema))
  }

  override def inputsOrEmptyList: Array[String] = Array(input)

  override def prepareInstance(): Unit =
    executionPlan.fillTasks(createTaskStreams(), createTaskNames(countParallelism, name))

  override def countParallelism: Int =
    castParallelismToNumber(getStreamsPartitions(streams))

  override def createStreams(): Unit =
    getStreams(Array(input)).foreach(_.create())

  override def getInputsWithoutStreamMode: Array[String] = Array(clearStreamFromMode(input))

  override val streams: Array[String] = getInputsWithoutStreamMode ++ outputs
}
