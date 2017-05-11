package com.bwsw.sj.common.rest.model.module

import java.util.Calendar

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.instance.{FrameworkStage, InstanceDomain}
import com.bwsw.sj.common.dal.model.stream.{KafkaStreamDomain, StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.utils.SjStreamUtils._
import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonIgnore

import scala.collection.JavaConverters._

class InstanceApi {
  var moduleName: String = _
  var moduleVersion: String = _
  var moduleType: String = _
  var stage: FrameworkStage = FrameworkStage(EngineLiterals.toHandle, Calendar.getInstance().getTime)
  var status: String = EngineLiterals.ready
  var name: String = _
  var description: String = RestLiterals.defaultDescription
  var parallelism: Any = 1
  var options: Map[String, Any] = Map()
  var perTaskCores: Double = 1.0
  var perTaskRam: Int = 1024
  var jvmOptions: Map[String, String] = Map()
  var nodeAttributes: Map[String, String] = Map()
  var coordinationService: String = _
  var environmentVariables: Map[String, String] = Map()
  var performanceReportingInterval: Long = 60000
  var engine: String = _
  var restAddress: String = _

  @JsonIgnore
  def asModelInstance(): InstanceDomain = ???

  @JsonIgnore
  protected def fillModelInstance(modelInstance: InstanceDomain): Unit = {
    val serializer = new JsonSerializer()

    modelInstance.status = this.status
    modelInstance.description = this.description
    modelInstance.parallelism = this.parallelism.asInstanceOf[Int]
    modelInstance.perTaskCores = this.perTaskCores
    modelInstance.perTaskRam = this.perTaskRam
    modelInstance.performanceReportingInterval = this.performanceReportingInterval
    modelInstance.options = serializer.serialize(this.options)
    modelInstance.jvmOptions = this.jvmOptions.asJava
    modelInstance.nodeAttributes = this.nodeAttributes.asJava
    modelInstance.environmentVariables = this.environmentVariables.asJava
    modelInstance.stage = this.stage
    modelInstance.restAddress = this.restAddress
  }

  @JsonIgnore
  def prepareInstance(moduleType: String,
                      moduleName: String,
                      moduleVersion: String,
                      engineName: String,
                      engineVersion: String): Unit = {
    this.engine = engineName + "-" + engineVersion
    this.moduleName = moduleName
    this.moduleVersion = moduleVersion
    this.moduleType = moduleType
  }

  @JsonIgnore
  def createStreams(): Unit = {}

  @JsonIgnore
  protected def castParallelismToNumber(partitions: Array[Int]): Unit = {
    val parallelism = this.parallelism match {
      case max: String => partitions.min
      case _ => this.parallelism
    }
    this.parallelism = parallelism
  }

  @JsonIgnore
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

  @JsonIgnore
  protected def getStreams(streamNames: Array[String]): Array[StreamDomain] = {
    val streamRepository = ConnectionRepository.getStreamRepository
    streamNames.flatMap(streamRepository.get)
  }

  @JsonIgnore
  protected def createTaskStreams(): Array[TaskStream] = {
    val streamRepository = ConnectionRepository.getStreamRepository
    val inputStreamsWithModes = splitStreamsAndModes(inputsOrEmptyList())
    inputStreamsWithModes.map(streamWithMode => {
      val partitions = getPartitions(streamWithMode.streamName, streamRepository)
      TaskStream(streamWithMode.streamName, streamWithMode.mode, partitions)
    })
  }

  @JsonIgnore
  protected def inputsOrEmptyList(): Array[String] = Array()

  private def splitStreamsAndModes(streamsWithModes: Array[String]): Array[StreamWithMode] = {
    streamsWithModes.map(x => {
      val name = clearStreamFromMode(x)
      val mode = getStreamMode(name)

      StreamWithMode(name, mode)
    })
  }

  private def getPartitions(streamName: String, streamRepository: GenericMongoRepository[StreamDomain]): Int = { //todo get rid of useless parameter streamRepository
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

  @JsonIgnore
  protected def createTaskNames(parallelism: Int, taskPrefix: String): Set[String] = {
    (0 until parallelism).map(x => createTaskName(taskPrefix, x)).toSet
  }

  private def createTaskName(taskPrefix: String, taskNumber: Int): String = {
    taskPrefix + "-task" + taskNumber
  }
}
