package com.bwsw.sj.crud.rest.utils

import java.util.Calendar

import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.common.DAL.model.module.{ExecutionPlan, Task, InstanceStage}
import com.bwsw.sj.common.utils.EngineConstants._
import com.bwsw.sj.common.rest.entities.module.{OutputInstanceMetadata, RegularInstanceMetadata, InstanceMetadata}
import com.bwsw.sj.crud.rest.utils.ConvertUtil._
import org.slf4j.{LoggerFactory, Logger}

import scala.collection.JavaConversions._
import scala.collection.mutable

trait ValidationUtils {
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  private case class InputStream(name: String, mode: String, partitionsCount: Int)

  private case class StreamProcess(currentPartition: Int, countFreePartitions: Int)

  def validateName(name: String) = {
    name.matches( """^([a-z][a-z0-9-]*)$""")
  }

  def validateServiceNamespace(namespace: String) = {
    namespace.matches( """^([a-z][a-z0-9_]*)$""")
  }

  /**
   * Create entity of instance for saving to database
   *
   * @param instanceMetadata - metadata of instance
   * @param partitionsCount - partitions count of input streams
   * @return
   */
  def createInstance(instanceMetadata: InstanceMetadata, partitionsCount: Map[String, Int], streams: Set[SjStream]) = {
    logger.debug(s"Instance ${instanceMetadata.name}. Create model object.")
    val executionPlan = createExecutionPlan(instanceMetadata, partitionsCount)
    val instance = instanceMetadataToInstance(instanceMetadata)
    instance.executionPlan = executionPlan
    val stages = scala.collection.mutable.Map[String, InstanceStage]()
    streams.foreach { stream =>
      val instanceStartTask = new InstanceStage
      instanceStartTask.state = toHandle
      instanceStartTask.datetime = Calendar.getInstance().getTime
      instanceStartTask.duration = 0
      stages.put(stream.name, instanceStartTask)
    }
    val instanceTask = new InstanceStage
    instanceTask.state = toHandle
    instanceTask.datetime = Calendar.getInstance().getTime
    instanceTask.duration = 0
    stages.put(instance.name, instanceTask)
    instance.stages = mapAsJavaMap(stages)
    Option(instance)
  }

  /**
   * Create execution plan for instance of module
   *
   * @param instance - instance for module
   * @return - execution plan of instance
   */
  private def createExecutionPlan(instance: InstanceMetadata, partitionsCount: Map[String, Int]) = {
    logger.debug(s"Instance ${instance.name}. Create an execution plan.")
    var inputStreams: Array[String] = Array()
    instance match {
      case regularInstanceMetadata: RegularInstanceMetadata =>
        inputStreams = regularInstanceMetadata.inputs
      case outputInstanceMetadata: OutputInstanceMetadata =>
        inputStreams = Array(outputInstanceMetadata.input)
      case _ =>
        throw new IllegalArgumentException(s"Can't create an execution plan for instance: '${instance.name}' of '${instance.getClass}' type")
    }

    val inputs = inputStreams.map { input =>
      val mode = getStreamMode(input)
      val name = input.replaceAll("/split|/full", "")
      InputStream(name, mode, partitionsCount(name))
    }
    val parallelism = instance.parallelism.asInstanceOf[Int]
    val tasks = (0 until parallelism)
      .map(x => instance.name + "-task" + x)
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
          case "full" => endPartition = startPartition + countFreePartitions
          case "split" =>
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

  def getStreamMode(name: String) = {
    if (name.contains("/full")) {
      "full"
    } else {
      "split"
    }
  }

  //todo create an abstract method - validate for all validators (which are object now, e.g. ProviderValidator)
}
