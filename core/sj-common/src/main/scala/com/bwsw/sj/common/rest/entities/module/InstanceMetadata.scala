package com.bwsw.sj.common.rest.entities.module

import java.util.Calendar

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.module.{Instance, InstanceStage}
import com.bwsw.sj.common.DAL.model.{KafkaSjStream, SjStream, TStreamSjStream, ZKService}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.SjStreamUtils._
import com.bwsw.sj.common.utils.{EngineLiterals, GeneratorLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonIgnore

import scala.collection.JavaConverters._

class InstanceMetadata {
  private var moduleName: String = null
  private var moduleVersion: String = null
  private var moduleType: String = null
  var stages = scala.collection.mutable.Map[String, InstanceStage]()
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
  protected def fillModelInstance(modelInstance: Instance) = {
    val serializer = new JsonSerializer()
    val serviceDAO = ConnectionRepository.getServiceManager

    modelInstance.status = this.status
    modelInstance.moduleName = this.moduleName
    modelInstance.moduleType = this.moduleType
    modelInstance.moduleVersion = this.moduleVersion
    modelInstance.name = this.name
    modelInstance.description = this.description
    modelInstance.parallelism = this.parallelism.asInstanceOf[Int]
    modelInstance.perTaskCores = this.perTaskCores
    modelInstance.perTaskRam = this.perTaskRam
    modelInstance.performanceReportingInterval = this.performanceReportingInterval
    modelInstance.engine = this.engine
    modelInstance.options = serializer.serialize(this.options)
    modelInstance.jvmOptions = this.jvmOptions.asJava
    modelInstance.nodeAttributes = this.nodeAttributes.asJava
    modelInstance.environmentVariables = this.environmentVariables.asJava
    modelInstance.stages = this.stages.asJava
    modelInstance.restAddress = this.restAddress

    val service = serviceDAO.get(this.coordinationService)
    if (service.isDefined && service.get.isInstanceOf[ZKService]) {
      modelInstance.coordinationService = service.get.asInstanceOf[ZKService]
    }
  }

  @JsonIgnore
  def prepareInstance(moduleType: String,
                      moduleName: String,
                      moduleVersion: String,
                      engineName: String,
                      engineVersion: String) = {
    this.engine = engineName + "-" + engineVersion
    this.moduleName = moduleName
    this.moduleVersion = moduleVersion
    this.moduleType = moduleType
  }

  @JsonIgnore
  def createStreams(): Unit = {}

  @JsonIgnore
  protected def castParallelismToNumber(partitions: Array[Int]) = {
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
  protected def fillStages(streamsWithGenerator: Array[String]) = {
    val streamsDAO = ConnectionRepository.getStreamService
    val initialStage = new InstanceStage(toHandle, Calendar.getInstance().getTime)
    val stageForLocalGenerator = new InstanceStage(started, Calendar.getInstance().getTime)
    this.stages.put(this.name, initialStage)

    streamsWithGenerator.foreach(stream => {
      val tstream = streamsDAO.get(stream).get.asInstanceOf[TStreamSjStream]
      tstream.generator.generatorType match {
        case GeneratorLiterals.localType => this.stages.put(stream, stageForLocalGenerator)
        case _ => this.stages.put(stream, initialStage)
      }
    })
  }

  @JsonIgnore
  protected def createTaskStreams() = {
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
  protected def createTaskNames(parallelism: Int, taskPrefix: String) = {
    (0 until parallelism).map(x => createTaskName(taskPrefix, x)).toSet
  }

  private def createTaskName(taskPrefix: String, taskNumber: Int) = {
    taskPrefix + "-task" + taskNumber
  }
}

