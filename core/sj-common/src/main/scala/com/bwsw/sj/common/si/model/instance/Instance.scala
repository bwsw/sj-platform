package com.bwsw.sj.common.si.model.instance

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.instance.{FrameworkStage, InstanceDomain}
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.model.stream.{KafkaStreamDomain, StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.rest.model.module.{StreamWithMode, TaskStream}
import com.bwsw.sj.common.utils.SjStreamUtils.clearStreamFromMode
import com.bwsw.sj.common.utils.{EngineLiterals, StreamLiterals}

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
               val status: String = EngineLiterals.ready) {
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
      performanceReportingInterval = performanceReportingInterval
    )
  }

  def countParallelism: Int = castParallelismToNumber(Array(1))

  def createStreams(): Unit = {}

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
  def from(instanceDomain: InstanceDomain): Instance = {
    val serializer = new JsonSerializer
    instanceDomain.moduleType match {
      case _ =>
        new Instance(
          instanceDomain.name,
          instanceDomain.description,
          instanceDomain.parallelism,
          serializer.deserialize[Map[String, Any]](instanceDomain.options),
          instanceDomain.perTaskCores,
          instanceDomain.perTaskRam,
          Map(instanceDomain.jvmOptions.asScala.toList: _*),
          Map(instanceDomain.nodeAttributes.asScala.toList: _*),
          instanceDomain.coordinationService.name,
          Map(instanceDomain.environmentVariables.asScala.toList: _*),
          instanceDomain.performanceReportingInterval,
          instanceDomain.moduleName,
          instanceDomain.moduleVersion,
          instanceDomain.moduleType,
          instanceDomain.engine,
          Option(instanceDomain.restAddress),
          instanceDomain.stage,
          instanceDomain.status)
    }
  }
}
