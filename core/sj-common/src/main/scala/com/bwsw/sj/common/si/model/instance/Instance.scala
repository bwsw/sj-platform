package com.bwsw.sj.common.si.model.instance

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.instance._
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.model.stream.{KafkaStreamDomain, StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.rest.model.module.{StreamWithMode, TaskStream}
import com.bwsw.sj.common.utils.SjStreamUtils.clearStreamFromMode
import com.bwsw.sj.common.utils.{AvroUtils, EngineLiterals, StreamLiterals}

import scala.collection.JavaConverters._

class Instance(val name: String,
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
               val moduleName: String,
               val moduleVersion: String,
               val moduleType: String,
               val engine: String,
               val restAddress: Option[String] = None,
               val stage: FrameworkStage = FrameworkStage(),
               val status: String = EngineLiterals.ready,
               val frameworkId: String = System.currentTimeMillis().toString) {

  def to: InstanceDomain = {
    val serializer = new JsonSerializer
    val serviceRepository = ConnectionRepository.getServiceRepository

    new InstanceDomain(
      name = name,
      moduleType = moduleType,
      moduleName = moduleName,
      moduleVersion = moduleVersion,
      engine = engine,
      coordinationService = serviceRepository.get(coordinationService).asInstanceOf[ZKServiceDomain],
      status = status,
      restAddress = restAddress.getOrElse(""),
      description = description,
      parallelism = countParallelism,
      options = serializer.serialize(options),
      perTaskCores = perTaskCores,
      perTaskRam = perTaskRam,
      jvmOptions = jvmOptions.asJava,
      nodeAttributes = nodeAttributes.asJava,
      environmentVariables = environmentVariables.asJava,
      stage = stage,
      performanceReportingInterval = performanceReportingInterval,
      frameworkId = frameworkId
    )
  }

  def countParallelism: Int = castParallelismToNumber(Array(1))

  def createStreams(): Unit = {}

  def prepareInstance(): Unit = {}

  protected def castParallelismToNumber(partitions: Array[Int]): Int = parallelism match {
    case "max" => partitions.min
    case x: Int => x
    case _ =>
      throw new IllegalStateException("Incorrect property 'parallelism'")
  }

  protected def getStreamsPartitions(streamNames: Array[String]): Array[Int] = {
    val streamRepository = ConnectionRepository.getStreamRepository
    val streams = streamRepository.getAll.filter(s => streamNames.contains(s.name))
    Array(streams.map { stream =>
      stream.streamType match {
        case StreamLiterals.`tstreamType` =>
          stream.asInstanceOf[TStreamStreamDomain].partitions
        case StreamLiterals.`kafkaStreamType` =>
          stream.asInstanceOf[KafkaStreamDomain].partitions
        case _ => 1
      }
    }: _*)
  }

  protected def getStreams(streamNames: Array[String]): Array[StreamDomain] = {
    val streamRepository = ConnectionRepository.getStreamRepository
    streamNames.flatMap(streamRepository.get)
  }

  protected def createTaskStreams(): Array[TaskStream] = {
    val streamRepository = ConnectionRepository.getStreamRepository
    val inputStreamsWithModes = splitStreamsAndModes(inputsOrEmptyList)
    inputStreamsWithModes.map(streamWithMode => {
      val partitions = getPartitions(streamWithMode.streamName, streamRepository)
      TaskStream(streamWithMode.streamName, streamWithMode.mode, partitions)
    })
  }

  def inputsOrEmptyList: Array[String] = Array()

  private def splitStreamsAndModes(streamsWithModes: Array[String]): Array[StreamWithMode] = {
    streamsWithModes.map(x => {
      val name = clearStreamFromMode(x)
      val mode = getStreamMode(name)

      StreamWithMode(name, mode)
    })
  }

  private def getPartitions(streamName: String, streamRepository: GenericMongoRepository[StreamDomain]): Int = {
    //todo get rid of useless parameter streamRepository
    val stream = streamRepository.get(streamName).get
    val partitions = stream.streamType match {
      case StreamLiterals.`tstreamType` =>
        stream.asInstanceOf[TStreamStreamDomain].partitions
      case StreamLiterals.`kafkaStreamType` =>
        stream.asInstanceOf[KafkaStreamDomain].partitions
    }

    partitions
  }

  private def getStreamMode(name: String): String = {
    if (name.contains(s"/${EngineLiterals.fullStreamMode}")) {
      EngineLiterals.fullStreamMode
    } else {
      EngineLiterals.splitStreamMode
    }
  }

  protected def createTaskNames(parallelism: Int, taskPrefix: String): Set[String] = {
    (0 until parallelism).map(x => createTaskName(taskPrefix, x)).toSet
  }

  private def createTaskName(taskPrefix: String, taskNumber: Int): String = {
    taskPrefix + "-task" + taskNumber
  }

}

object Instance {
  def from(instance: InstanceDomain): Instance = {
    val serializer = new JsonSerializer
    instance.moduleType match {
      case EngineLiterals.inputStreamingType =>
        val inputInstance = instance.asInstanceOf[InputInstanceDomain]

        new InputInstance(
          inputInstance.name,
          inputInstance.description,
          inputInstance.parallelism,
          serializer.deserialize[Map[String, Any]](inputInstance.options),
          inputInstance.perTaskCores,
          inputInstance.perTaskRam,
          Map(inputInstance.jvmOptions.asScala.toList: _*),
          Map(inputInstance.nodeAttributes.asScala.toList: _*),
          inputInstance.coordinationService.name,
          Map(inputInstance.environmentVariables.asScala.toList: _*),
          inputInstance.performanceReportingInterval,
          inputInstance.moduleName,
          inputInstance.moduleVersion,
          inputInstance.moduleType,
          inputInstance.engine,

          inputInstance.checkpointMode,
          inputInstance.checkpointInterval,
          inputInstance.outputs,
          inputInstance.lookupHistory,
          inputInstance.queueMaxSize,
          inputInstance.duplicateCheck,
          inputInstance.defaultEvictionPolicy,
          inputInstance.evictionPolicy,
          inputInstance.backupCount,
          inputInstance.asyncBackupCount,
          Map(inputInstance.tasks.asScala.toList: _*),

          Option(inputInstance.restAddress),
          inputInstance.stage,
          inputInstance.status,
          inputInstance.frameworkId)

      case EngineLiterals.batchStreamingType =>
        val x = instance.asInstanceOf[BatchInstanceDomain]

        new BatchInstance(
          x.name,
          x.description,
          x.parallelism,
          serializer.deserialize[Map[String, Any]](x.options),
          x.perTaskCores,
          x.perTaskRam,
          Map(x.jvmOptions.asScala.toList: _*),
          Map(x.nodeAttributes.asScala.toList: _*),
          x.coordinationService.name,
          Map(x.environmentVariables.asScala.toList: _*),
          x.performanceReportingInterval,
          x.moduleName,
          x.moduleVersion,
          x.moduleType,
          x.engine,

          x.inputs,
          x.outputs,
          x.window,
          x.slidingInterval,
          x.startFrom,
          x.stateManagement,
          x.stateFullCheckpoint,
          x.eventWaitIdleTime,
          AvroUtils.jsonToSchema(x.inputAvroSchema),
          x.executionPlan,

          Option(x.restAddress),
          x.stage,
          x.status,
          x.frameworkId)

      case EngineLiterals.regularStreamingType =>
        val x = instance.asInstanceOf[RegularInstanceDomain]

        new RegularInstance(
          x.name,
          x.description,
          x.parallelism,
          serializer.deserialize[Map[String, Any]](x.options),
          x.perTaskCores,
          x.perTaskRam,
          Map(x.jvmOptions.asScala.toList: _*),
          Map(x.nodeAttributes.asScala.toList: _*),
          x.coordinationService.name,
          Map(x.environmentVariables.asScala.toList: _*),
          x.performanceReportingInterval,
          x.moduleName,
          x.moduleVersion,
          x.moduleType,
          x.engine,

          x.inputs,
          x.outputs,
          x.checkpointMode,
          x.checkpointInterval,
          x.startFrom,
          x.stateManagement,
          x.stateFullCheckpoint,
          x.eventWaitIdleTime,
          AvroUtils.jsonToSchema(x.inputAvroSchema),
          x.executionPlan,

          Option(x.restAddress),
          x.stage,
          x.status,
          x.frameworkId)

      case EngineLiterals.outputStreamingType =>
        val x = instance.asInstanceOf[OutputInstanceDomain]

        new OutputInstance(
          x.name,
          x.description,
          x.parallelism,
          serializer.deserialize[Map[String, Any]](x.options),
          x.perTaskCores,
          x.perTaskRam,
          Map(x.jvmOptions.asScala.toList: _*),
          Map(x.nodeAttributes.asScala.toList: _*),
          x.coordinationService.name,
          Map(x.environmentVariables.asScala.toList: _*),
          x.performanceReportingInterval,
          x.moduleName,
          x.moduleVersion,
          x.moduleType,
          x.engine,

          x.checkpointMode,
          x.checkpointInterval,
          x.inputs.head,
          x.outputs.head,
          x.startFrom,
          AvroUtils.jsonToSchema(x.inputAvroSchema),
          x.executionPlan,

          Option(x.restAddress),
          x.stage,
          x.status,
          x.frameworkId)

      case _ =>
        new Instance(
          instance.name,
          instance.description,
          instance.parallelism,
          serializer.deserialize[Map[String, Any]](instance.options),
          instance.perTaskCores,
          instance.perTaskRam,
          Map(instance.jvmOptions.asScala.toList: _*),
          Map(instance.nodeAttributes.asScala.toList: _*),
          instance.coordinationService.name,
          Map(instance.environmentVariables.asScala.toList: _*),
          instance.performanceReportingInterval,
          instance.moduleName,
          instance.moduleVersion,
          instance.moduleType,
          instance.engine,
          Option(instance.restAddress),
          instance.stage,
          instance.status)
    }
  }
}
