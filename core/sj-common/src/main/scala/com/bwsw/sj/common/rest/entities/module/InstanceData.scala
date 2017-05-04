package com.bwsw.sj.common.rest.entities.module

import java.util.Calendar

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.module.{FrameworkStage, Instance}
import com.bwsw.sj.common.DAL.model.stream.{KafkaSjStream, SjStream, TStreamSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.rest.entities.Data
import com.bwsw.sj.common.utils.SjStreamUtils._
import com.bwsw.sj.common.utils.{EngineLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonIgnore

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

class InstanceData extends Data {
  var moduleName: String = null
  var moduleVersion: String = null
  var moduleType: String = null
  var stage = FrameworkStage(EngineLiterals.toHandle, Calendar.getInstance().getTime)
  var status: String = EngineLiterals.ready
  var name: String = null
  var description: String = "No description"
  var parallelism: Any = 1
  var options: Map[String, Any] = Map()
  var perTaskCores: Double = 1.0
  var perTaskRam: Int = 1024
  var jvmOptions: Map[String, String] = Map()
  var nodeAttributes: Map[String, String] = Map()
  var coordinationService: String = null
  var environmentVariables: Map[String, String] = Map()
  var performanceReportingInterval: Long = 60000
  var engine: String = null
  var restAddress: String = null

  @JsonIgnore
  def asModelInstance(): Instance = ???

  @JsonIgnore
  protected def fillModelInstance(modelInstance: Instance): Unit = {
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
    val streamsDAO = ConnectionRepository.getStreamService
    val streams = streamsDAO.getAll.filter(s => streamNames.contains(s.name))
    Array(streams.map { stream =>
      stream.streamType match {
        case StreamLiterals.`tstreamType` =>
          stream.asInstanceOf[TStreamSjStream].partitions
        case StreamLiterals.`kafkaStreamType` =>
          stream.asInstanceOf[KafkaSjStream].partitions
      }
    }: _*)
  }

  @JsonIgnore
  protected def getStreams(streamNames: Array[String]): Array[SjStream] = {
    val streamsDAO = ConnectionRepository.getStreamService
    streamNames.flatMap(streamsDAO.get)
  }

  @JsonIgnore
  protected def createTaskStreams(): Array[TaskStream] = {
    val streamDAO = ConnectionRepository.getStreamService
    val inputStreamsWithModes = splitStreamsAndModes(inputsOrEmptyList())
    inputStreamsWithModes.map(streamWithMode => {
      val partitions = getPartitions(streamWithMode.streamName, streamDAO)
      TaskStream(streamWithMode.streamName, streamWithMode.mode, partitions)
    })
  }

  @JsonIgnore
  protected def inputsOrEmptyList(): Array[String] = Array()

  private def splitStreamsAndModes(streamsWithModes: Array[String]) = {
    streamsWithModes.map(x => {
      val name = clearStreamFromMode(x)
      val mode = getStreamMode(name)

      StreamWithMode(name, mode)
    })
  }

  private def getPartitions(streamName: String, streamDAO: GenericMongoService[SjStream]) = {
    val stream = streamDAO.get(streamName).get
    val partitions = stream.streamType match {
      case StreamLiterals.`tstreamType` =>
        stream.asInstanceOf[TStreamSjStream].partitions
      case StreamLiterals.`kafkaStreamType` =>
        stream.asInstanceOf[KafkaSjStream].partitions
    }

    partitions
  }

  private def getStreamMode(name: String) = {
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

  private def createTaskName(taskPrefix: String, taskNumber: Int) = {
    taskPrefix + "-task" + taskNumber
  }

  override def validate(): ArrayBuffer[String] = ??? //todo
}
