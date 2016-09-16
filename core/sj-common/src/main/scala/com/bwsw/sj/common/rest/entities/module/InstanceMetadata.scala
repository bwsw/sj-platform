package com.bwsw.sj.common.rest.entities.module

import java.util.Calendar

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.module.{InstanceStage, ExecutionPlan, Instance, Task}
import com.bwsw.sj.common.DAL.model.{SjStream, KafkaSjStream, TStreamSjStream, ZKService}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.{EngineLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonProperty

import scala.collection.JavaConversions._
import scala.collection.mutable

class InstanceMetadata {
  private var moduleName: String = null
  private var moduleVersion: String = null
  private var moduleType: String = null
  var status: String = null
  var name: String = null
  var description: String = "No description"
  @JsonProperty("checkpoint-mode") var checkpointMode: String = null
  @JsonProperty("checkpoint-interval") var checkpointInterval: Long = 0L
  var parallelism: Any = 1
  var options: Map[String, Any] = Map()
  @JsonProperty("per-task-cores") var perTaskCores: Double = 1.0
  @JsonProperty("per-task-ram") var perTaskRam: Int = 1024
  @JsonProperty("jvm-options") var jvmOptions: Map[String, String] = Map()
  @JsonProperty("node-attributes") var nodeAttributes: Map[String, String] = Map()
  @JsonProperty("coordination-service") var coordinationService: String = null
  @JsonProperty("environment-variables") var environmentVariables: Map[String, String] = Map()
  @JsonProperty("performance-reporting-interval") var performanceReportingInterval: Long = 60000
  var engine: String = null

  def asModelInstance(): Instance = ???

  protected def fillModelInstance(modelInstance: Instance) = {
    val serializer = new JsonSerializer()
    val serviceDAO = ConnectionRepository.getServiceManager

    modelInstance.status = this.status
    modelInstance.moduleName = this.moduleName
    modelInstance.moduleType = this.moduleType
    modelInstance.moduleVersion = this.moduleVersion
    modelInstance.name = this.name
    modelInstance.description = this.description
    modelInstance.checkpointMode = this.checkpointMode
    modelInstance.checkpointInterval = this.checkpointInterval
    modelInstance.parallelism = this.parallelism.asInstanceOf[Int]
    modelInstance.perTaskCores = this.perTaskCores
    modelInstance.perTaskRam = this.perTaskRam
    modelInstance.performanceReportingInterval = this.performanceReportingInterval
    modelInstance.engine = this.engine
    modelInstance.options = serializer.serialize(this.options)
    modelInstance.jvmOptions = mapAsJavaMap(this.jvmOptions)
    modelInstance.nodeAttributes = mapAsJavaMap(this.nodeAttributes)
    modelInstance.environmentVariables = mapAsJavaMap(this.environmentVariables)
    val service = serviceDAO.get(this.coordinationService)
    if (service.isDefined && service.get.isInstanceOf[ZKService]) {
      modelInstance.coordinationService = service.get.asInstanceOf[ZKService]
    }
  }

  def fillInstance(moduleType: String,
                   moduleName: String,
                   moduleVersion: String,
                   engineName: String,
                   engineVersion: String) = {
    this.engine = engineName + "-" + engineVersion
    this.moduleName = moduleName
    this.moduleVersion = moduleVersion
    this.moduleType = moduleType
    this.status = ready

    this.asModelInstance()
  }

  def createStreams(): Unit = ???

  protected def clearStreamFromMode(streamName: String) = {
    streamName.replaceAll(s"/${EngineLiterals.splitStreamMode}|/${EngineLiterals.fullStreamMode}", "")
  }

  protected def createStages(streamsWithGenerator: Array[String]) = {
    val stages = scala.collection.mutable.Map[String, InstanceStage]()
    streamsWithGenerator.foreach { stream =>
      val instanceStartTask = new InstanceStage
      instanceStartTask.state = toHandle
      instanceStartTask.datetime = Calendar.getInstance().getTime
      instanceStartTask.duration = 0
      stages.put(stream, instanceStartTask)
    }
    val instanceTask = new InstanceStage
    instanceTask.state = toHandle
    instanceTask.datetime = Calendar.getInstance().getTime
    instanceTask.duration = 0
    stages.put(this.name, instanceTask)

    stages
  }

  protected def createExecutionPlan() = {
    case class InputStream(name: String, mode: String, partitionsCount: Int)
    case class StreamProcess(currentPartition: Int, countFreePartitions: Int)
    val streamDAO = ConnectionRepository.getStreamService

    val inputStreams = getInputs()
    val inputs = inputStreams.map { input =>
      val stream = streamDAO.get(input).get
      val partition = stream.streamType match {
        case StreamLiterals.`tStreamType` =>
          stream.asInstanceOf[TStreamSjStream].partitions
        case StreamLiterals.`kafkaStreamType` =>
          stream.asInstanceOf[KafkaSjStream].partitions
      }
      val mode = getStreamMode(input)
      val name = input.replaceAll(s"/${EngineLiterals.splitStreamMode}|/${EngineLiterals.fullStreamMode}", "")
      InputStream(name, mode, partition)
    }
    val parallelism = this.parallelism.asInstanceOf[Int]
    val tasks = (0 until parallelism)
      .map(x => this.name + "-task" + x)
      .map(x => x -> inputs)

    val executionPlan = mutable.Map[String, Task]()
    val streams = mutable.Map(inputs.map(x => x.name -> StreamProcess(0, x.partitionsCount)).toSeq: _*)

    var tasksNotProcessed = tasks.size
    tasks.foreach { task =>
      val list = task._2.map { inputStream =>
        val stream = streams(inputStream.name)
        val countFreePartitions = stream.countFreePartitions
        val startPartition = stream.currentPartition
        var endPartition = startPartition + countFreePartitions
        inputStream.mode match {
          case EngineLiterals.fullStreamMode => endPartition = startPartition + countFreePartitions
          case EngineLiterals.splitStreamMode =>
            val cntTaskStreamPartitions = countFreePartitions / tasksNotProcessed
            streams.update(inputStream.name, StreamProcess(startPartition + cntTaskStreamPartitions, countFreePartitions - cntTaskStreamPartitions))
            if (Math.abs(cntTaskStreamPartitions - countFreePartitions) >= cntTaskStreamPartitions) {
              endPartition = startPartition + cntTaskStreamPartitions
            }
        }

        inputStream.name -> Array(startPartition, endPartition - 1)
      }
      tasksNotProcessed -= 1
      val planTask = new Task
      planTask.inputs = mapAsJavaMap(Map(list.toSeq: _*))
      executionPlan.put(task._1, planTask)
    }
    val execPlan = new ExecutionPlan
    execPlan.tasks = mapAsJavaMap(executionPlan)
    execPlan
  }

  protected def getInputs(): Array[String] = {
    throw new IllegalArgumentException(s"Can't get an inputs for instance: '${this.name}' of '${this.getClass}' type")
  }

  private def getStreamMode(name: String) = {
    if (name.contains(s"/${EngineLiterals.fullStreamMode}")) {
      EngineLiterals.fullStreamMode
    } else {
      EngineLiterals.splitStreamMode
    }
  }

  protected def castParallelismToNumber(streams: Set[String]) = {
    val parallelism = this.parallelism match {
      case max: String =>
        val partitions = getStreamsPartitions(streams)

        partitions.min
      case _ => this.parallelism
    }

    this.parallelism = parallelism
  }

  private def getStreamsPartitions(streamNames: Set[String]): Array[Int] = {
    val streamsDAO = ConnectionRepository.getStreamService
    val streams = streamsDAO.getAll.filter(s => streamNames.contains(s.name))
    Array(streams.map { stream =>
      stream.streamType match {
        case StreamLiterals.`tStreamType` =>
          stream.asInstanceOf[TStreamSjStream].partitions
        case StreamLiterals.`kafkaStreamType` =>
          stream.asInstanceOf[KafkaSjStream].partitions
      }
    }: _*)
  }

  protected def getStreams(streamNames: Array[String]): Array[SjStream] = {
    val streamsDAO = ConnectionRepository.getStreamService
    streamNames.flatMap(streamsDAO.get)
  }
}

