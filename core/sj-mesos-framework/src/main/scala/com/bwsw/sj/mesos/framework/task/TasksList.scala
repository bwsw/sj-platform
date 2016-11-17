package com.bwsw.sj.mesos.framework.task

import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.mesos.framework.schedule.FrameworkUtil
import scala.collection.JavaConverters._


import scala.collection.mutable

object TasksList {

  private val tasksToLaunch: mutable.ListBuffer[String] = mutable.ListBuffer()
  private val listTasks : mutable.Map[String, Task] = mutable.Map()
  var message: String = "Initialization"
  var availablePorts: collection.mutable.ListBuffer[Long] = collection.mutable.ListBuffer()

  var perTaskCores: Double = 0.0
  var perTaskMem: Double = 0.0
  var perTaskPortsCount: Int = 0

  def newTask(taskId: String) = {
    val task = new Task(taskId)
    listTasks += taskId -> task
    tasksToLaunch += taskId
  }

  def getList = {
    listTasks.values
  }

  def getTask(taskId: String): Task = {
    listTasks(taskId)
  }

  def addToLaunch(taskId: String) = {
    tasksToLaunch += taskId
  }

  def toLaunch: mutable.ListBuffer[String] = {
    tasksToLaunch
  }

  def launched(taskId: String) = {
    tasksToLaunch -= taskId
  }

  def toJson: Map[String, Any] = {
    Map(("tasks", listTasks.values.map(_.toJson)),
      ("message", message))
  }

  def apply(taskId: String): Option[Task] = {
    listTasks.get(taskId)
  }

  def clearAvailablePorts() = {
    availablePorts.remove(0, availablePorts.length)
  }

  def count = {
    toLaunch.size
  }

  def prepare(instance: Instance) = {
    perTaskCores = FrameworkUtil.instance.perTaskCores
    perTaskMem = FrameworkUtil.instance.perTaskRam
    perTaskPortsCount = FrameworkUtil.getCountPorts(FrameworkUtil.instance)

    val tasks = FrameworkUtil.instance.moduleType match {
      case EngineLiterals.inputStreamingType =>
        (0 until FrameworkUtil.instance.parallelism).map(tn => FrameworkUtil.instance.name + "-task" + tn)
      case _ =>
        val executionPlan = FrameworkUtil.instance match {
          case regularInstance: RegularInstance => regularInstance.executionPlan
          case outputInstance: OutputInstance => outputInstance.executionPlan
          case windowedInstance: WindowedInstance => windowedInstance.executionPlan
        }
        executionPlan.tasks.asScala.keys
    }
    tasks.foreach(task => newTask(task))
  }

}
