package com.bwsw.sj.common.rest.entities.module

import java.util

import com.bwsw.sj.common.DAL.model.module.Task
import com.bwsw.sj.common.utils.EngineLiterals

import scala.collection.JavaConversions._

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

  def fillTasks(taskStreams: Array[TaskStream], taskNames: Set[String]) = {
    var notProcessedTasks = taskNames.size

    taskNames.foreach { taskName =>
      val list = taskStreams.map { taskStream =>
        val availablePartitionsCount = taskStream.availablePartitionsCount
        val startPartition = taskStream.currentPartition
        var endPartition = startPartition + availablePartitionsCount
        taskStream.mode match {
          case EngineLiterals.fullStreamMode => endPartition = startPartition + availablePartitionsCount
          case EngineLiterals.splitStreamMode =>
            val cntTaskStreamPartitions = availablePartitionsCount / notProcessedTasks
            taskStream.availablePartitionsCount = taskStream.availablePartitionsCount - cntTaskStreamPartitions
            taskStream.currentPartition = startPartition + cntTaskStreamPartitions
            if (Math.abs(cntTaskStreamPartitions - availablePartitionsCount) >= cntTaskStreamPartitions) {
              endPartition = startPartition + cntTaskStreamPartitions
            }
        }

        taskStream.name -> Array(startPartition, endPartition - 1)
      }.toMap
      notProcessedTasks -= 1
      this.tasks.put(taskName, new Task(list))
    }

    this
  }
}
