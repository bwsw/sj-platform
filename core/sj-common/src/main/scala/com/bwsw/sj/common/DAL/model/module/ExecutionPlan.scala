package com.bwsw.sj.common.DAL.model.module

import java.util

import com.bwsw.sj.common.DAL.model.{KafkaSjStream, TStreamSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{EngineLiterals, StreamLiterals}

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
 * Entity for execution plan of module instance
 *
 *
 * @author Kseniya Tomskikh
 */
class ExecutionPlan {
  var tasks: java.util.Map[String, Task] = new util.HashMap()

  def this(tasks: java.util.Map[String, Task]) = {
    this()
    this.tasks = tasks
  }

  def fillTasks(inputStreamsWithModes: Array[String], parallelism: Int, taskPrefix: String) = {
    case class InputStream(name: String, mode: String, partitionsCount: Int)
    case class StreamProcess(currentPartition: Int, countFreePartitions: Int)
    val streamDAO = ConnectionRepository.getStreamService
    val inputs = inputStreamsWithModes.map { input =>
      val name = clearStreamFromMode(input)
      val stream = streamDAO.get(name).get
      val partition = stream.streamType match {
        case StreamLiterals.`tStreamType` =>
          stream.asInstanceOf[TStreamSjStream].partitions
        case StreamLiterals.`kafkaStreamType` =>
          stream.asInstanceOf[KafkaSjStream].partitions
      }
      val mode = getStreamMode(input)
      InputStream(name, mode, partition)
    }

    val tasks = (0 until parallelism)
      .map(x => taskPrefix + "-task" + x)
      .map(x => x -> inputs)

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
      }.toMap
      tasksNotProcessed -= 1
      this.tasks.put(task._1, new Task(list))
    }

    this
  }

  private def getStreamMode(name: String) = {
    if (name.contains(s"/${EngineLiterals.fullStreamMode}")) {
      EngineLiterals.fullStreamMode
    } else {
      EngineLiterals.splitStreamMode
    }
  }

  private def clearStreamFromMode(streamName: String) = {
    streamName.replaceAll(s"/${EngineLiterals.splitStreamMode}|/${EngineLiterals.fullStreamMode}", "")
  }
}
