package com.bwsw.sj.mesos.framework

import com.bwsw.sj.common.DAL.model.Task
import com.bwsw.sj.common.DAL.model.{Task => ormTask}

import scala.collection.mutable

/**
  * Created by diryavkin_dn on 18.05.16.
  */
object TasksList{
  class Task(task_id: String){
    val id: String = task_id
    var state: String = "TASK_STAGING"
    var state_changed: String = ""
    var reason: String = ""
    var node: String = ""
    var last_node: String = ""
    val description: ormTask = null

    def update(state: String = state,
               state_changed: String = state_changed,
               reason: String = reason,
               node: String = node,
               last_node: String = last_node) = {
      this.state = state
      this.state_changed = state_changed
      this.reason = reason
      this.node = node
      this.last_node = last_node
    }

    def toJson: Map[String, Any] = {
      Map(("id", id), ("state", state),
        ("state-change", state_changed), ("reason", reason),
        ("node", node),("last-node", last_node))
    }
  }
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

  def getTask(taskId: String): Option[Task] = {
    listTasks.get(taskId)
  }

  def addToLaunch(taskId: String) = {
    tasksToLaunch+=taskId
  }

  def toLaunch: mutable.ListBuffer[String] = {
    tasksToLaunch
  }

  def launched(taskId: String) = {
    tasksToLaunch-=taskId
  }

  def toJson: Map[String, Any] = {
    Map(("tasks", listTasks.values.map{x => x.toJson}),
      ("message", message))
  }

  def apply(taskId: String): Option[Task] = {
    listTasks.get(taskId)
  }
}

