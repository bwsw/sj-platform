package com.bwsw.sj.common.si.model.instance

import com.bwsw.sj.common.dal.model.instance._
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.model.stream.{KafkaStreamDomain, StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.rest.model.module.{StreamWithMode, TaskStream}
import com.bwsw.sj.common.utils.StreamUtils.clearStreamFromMode
import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals, StreamLiterals}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.JavaConverters._

class Instance(val name: String,
               val description: String = RestLiterals.defaultDescription,
               val parallelism: Any = 1,
               val options: String = "{}",
               val perTaskCores: Double = 1,
               val perTaskRam: Int = 1024,
               val jvmOptions: Map[String, String] = Map(),
               val nodeAttributes: Map[String, String] = Map(),
               val coordinationService: String,
               val environmentVariables: Map[String, String] = Map(),
               val performanceReportingInterval: Long = 60000,
               val moduleName: String,
               val moduleVersion: String,
               val moduleType: String,
               val engine: String,
               var restAddress: Option[String] = None,
               val stage: FrameworkStage = FrameworkStage(),
               var status: String = EngineLiterals.ready,
               val frameworkId: String = System.currentTimeMillis().toString,
               val outputs: Array[String] = Array())
              (implicit injector: Injector) {

  protected val connectionRepository: ConnectionRepository = inject[ConnectionRepository]
  protected val streamRepository: GenericMongoRepository[StreamDomain] = connectionRepository.getStreamRepository

  def to(): InstanceDomain = {
    val serviceRepository = connectionRepository.getServiceRepository

    new InstanceDomain(
      name = name,
      moduleType = moduleType,
      moduleName = moduleName,
      moduleVersion = moduleVersion,
      engine = engine,
      coordinationService = serviceRepository.get(coordinationService).get.asInstanceOf[ZKServiceDomain],
      status = status,
      restAddress = restAddress.getOrElse(""),
      description = description,
      parallelism = countParallelism,
      options = options,
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

  /**
    * Creates streams after instance creating
    */
  def createStreams(): Unit = {}

  def prepareInstance(): Unit = {}

  def getInputsWithoutStreamMode: Array[String] = Array()

  val streams: Array[String] = outputs

  protected def castParallelismToNumber(partitions: Array[Int]): Int = parallelism match {
    case "max" => partitions.min
    case x: Int => x
    case _ =>
      throw new IllegalStateException("Incorrect property 'parallelism'")
  }

  protected def getStreamsPartitions(streamNames: Array[String]): Array[Int] = {
    val streams = streamNames.flatMap(x => streamRepository.get(x))
    streams.map(getPartitions)
  }

  private def getPartitions(stream: StreamDomain): Int = {
    stream.streamType match {
      case StreamLiterals.`tstreamType` =>
        stream.asInstanceOf[TStreamStreamDomain].partitions
      case StreamLiterals.`kafkaStreamType` =>
        stream.asInstanceOf[KafkaStreamDomain].partitions
      case _ => 1
    }
  }

  protected def getStreams(streamNames: Array[String]): Array[StreamDomain] =
    streamNames.flatMap(streamRepository.get)

  protected def createTaskStreams(): Array[TaskStream] = {
    val inputStreamsWithModes = splitStreamsAndModes(inputsOrEmptyList)
    inputStreamsWithModes.map(streamWithMode => {
      val partitions = getPartitions(streamWithMode.streamName)
      TaskStream(streamWithMode.streamName, streamWithMode.mode, partitions)
    })
  }

  protected def inputsOrEmptyList: Array[String] = Array()

  private def splitStreamsAndModes(streamsWithModes: Array[String]): Array[StreamWithMode] = {
    streamsWithModes.map(x => {
      val name = clearStreamFromMode(x)
      val mode = getStreamMode(name)

      StreamWithMode(name, mode)
    })
  }

  private def getPartitions(streamName: String): Int = {
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
  def from(instance: InstanceDomain)(implicit injector: Injector): Instance = {
    instance.moduleType match {
      case EngineLiterals.inputStreamingType =>
        val inputInstance = instance.asInstanceOf[InputInstanceDomain]

        new InputInstance(
          inputInstance.name,
          inputInstance.description,
          inputInstance.parallelism,
          inputInstance.options,
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
          inputInstance.tasks.asScala,

          Option(inputInstance.restAddress),
          inputInstance.stage,
          inputInstance.status,
          inputInstance.frameworkId)

      case EngineLiterals.batchStreamingType =>
        val batchInstance = instance.asInstanceOf[BatchInstanceDomain]

        new BatchInstance(
          batchInstance.name,
          batchInstance.description,
          batchInstance.parallelism,
          batchInstance.options,
          batchInstance.perTaskCores,
          batchInstance.perTaskRam,
          Map(batchInstance.jvmOptions.asScala.toList: _*),
          Map(batchInstance.nodeAttributes.asScala.toList: _*),
          batchInstance.coordinationService.name,
          Map(batchInstance.environmentVariables.asScala.toList: _*),
          batchInstance.performanceReportingInterval,
          batchInstance.moduleName,
          batchInstance.moduleVersion,
          batchInstance.moduleType,
          batchInstance.engine,

          batchInstance.inputs,
          batchInstance.outputs,
          batchInstance.window,
          batchInstance.slidingInterval,
          batchInstance.startFrom,
          batchInstance.stateManagement,
          batchInstance.stateFullCheckpoint,
          batchInstance.eventWaitIdleTime,
          batchInstance.inputAvroSchema,
          batchInstance.executionPlan,

          Option(batchInstance.restAddress),
          batchInstance.stage,
          batchInstance.status,
          batchInstance.frameworkId)

      case EngineLiterals.regularStreamingType =>
        val regularInstance = instance.asInstanceOf[RegularInstanceDomain]

        new RegularInstance(
          regularInstance.name,
          regularInstance.description,
          regularInstance.parallelism,
          regularInstance.options,
          regularInstance.perTaskCores,
          regularInstance.perTaskRam,
          Map(regularInstance.jvmOptions.asScala.toList: _*),
          Map(regularInstance.nodeAttributes.asScala.toList: _*),
          regularInstance.coordinationService.name,
          Map(regularInstance.environmentVariables.asScala.toList: _*),
          regularInstance.performanceReportingInterval,
          regularInstance.moduleName,
          regularInstance.moduleVersion,
          regularInstance.moduleType,
          regularInstance.engine,

          regularInstance.inputs,
          regularInstance.outputs,
          regularInstance.checkpointMode,
          regularInstance.checkpointInterval,
          regularInstance.startFrom,
          regularInstance.stateManagement,
          regularInstance.stateFullCheckpoint,
          regularInstance.eventWaitIdleTime,
          regularInstance.inputAvroSchema,
          regularInstance.executionPlan,

          Option(regularInstance.restAddress),
          regularInstance.stage,
          regularInstance.status,
          regularInstance.frameworkId)

      case EngineLiterals.outputStreamingType =>
        val outputInstance = instance.asInstanceOf[OutputInstanceDomain]

        new OutputInstance(
          outputInstance.name,
          outputInstance.description,
          outputInstance.parallelism,
          outputInstance.options,
          outputInstance.perTaskCores,
          outputInstance.perTaskRam,
          Map(outputInstance.jvmOptions.asScala.toList: _*),
          Map(outputInstance.nodeAttributes.asScala.toList: _*),
          outputInstance.coordinationService.name,
          Map(outputInstance.environmentVariables.asScala.toList: _*),
          outputInstance.performanceReportingInterval,
          outputInstance.moduleName,
          outputInstance.moduleVersion,
          outputInstance.moduleType,
          outputInstance.engine,

          outputInstance.checkpointMode,
          outputInstance.checkpointInterval,
          outputInstance.inputs.head,
          outputInstance.outputs.head,
          outputInstance.startFrom,
          outputInstance.inputAvroSchema,
          outputInstance.executionPlan,

          Option(outputInstance.restAddress),
          outputInstance.stage,
          outputInstance.status,
          outputInstance.frameworkId)

      case _ =>
        new Instance(
          instance.name,
          instance.description,
          instance.parallelism,
          instance.options,
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
