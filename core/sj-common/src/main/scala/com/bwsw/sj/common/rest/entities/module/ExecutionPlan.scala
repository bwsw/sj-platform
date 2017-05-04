package com.bwsw.sj.common.rest.entities.module

import java.util

import com.bwsw.sj.common.DAL.model.module.Task
import com.bwsw.sj.common.utils.EngineLiterals
import com.fasterxml.jackson.annotation.JsonIgnore

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

  @JsonIgnore
  def fillTasks(taskStreams: Array[TaskStream], taskNames: Set[String]): ExecutionPlan = {
    var notProcessedTasks = taskNames.size

    taskNames.foreach(taskName => {
      val task = createTask(taskStreams, notProcessedTasks)
      this.tasks.put(taskName, task)
      notProcessedTasks -= 1
    })

    this
  }

  private def createTask(taskStreams: Array[TaskStream], notProcessedTasks: Int): Task = {
    val task= new Task()
    taskStreams.foreach(taskStream => {
      task.addInput(taskStream.name, createPartitionsInterval(taskStream, notProcessedTasks))
    })

    task
  }

  private def createPartitionsInterval(taskStream: TaskStream, notProcessedTasks: Int): Array[Int] = {
    val startPartition = taskStream.currentPartition
    val endPartition = getEndPartition(taskStream, notProcessedTasks)
    val interval = Array(startPartition, endPartition - 1)

    interval
  }

  private def getEndPartition(taskStream: TaskStream, notProcessedTasks: Int): Int = {
    val availablePartitionsCount = taskStream.availablePartitionsCount
    val startPartition = taskStream.currentPartition
    var endPartition = startPartition + availablePartitionsCount
    taskStream.mode match {
      case EngineLiterals.splitStreamMode =>
        val cntTaskStreamPartitions = availablePartitionsCount / notProcessedTasks
        taskStream.availablePartitionsCount -= cntTaskStreamPartitions
        taskStream.currentPartition += cntTaskStreamPartitions
        if (Math.abs(cntTaskStreamPartitions - availablePartitionsCount) >= cntTaskStreamPartitions) {
          endPartition = startPartition + cntTaskStreamPartitions
        }

      case EngineLiterals.fullStreamMode =>
    }

    endPartition
  }
}
