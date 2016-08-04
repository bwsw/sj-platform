package com.bwsw.sj.mesos.framework.task

import scala.collection.mutable

/**
  * Created by diryavkin_dn on 18.05.16.
  */
object TasksList {

  private val tasksToLaunch: mutable.ListBuffer[String] = mutable.ListBuffer()
  private val listTasks : mutable.Map[String, Task] = mutable.Map()
  var message: String = "Initialization"

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
}

