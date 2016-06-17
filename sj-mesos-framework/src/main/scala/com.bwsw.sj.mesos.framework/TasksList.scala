package com.bwsw.sj.mesos.framework

import com.bwsw.sj.common.DAL.model.module.{Task => ormTask}
import scala.collection.mutable
import java.sql.Timestamp
import org.joda.time.DateTime

/**
  * Created by diryavkin_dn on 18.05.16.
  */
object TasksList{
  val usedPorts:collection.mutable.Map[String, mutable.Map[String, collection.mutable.ListBuffer[Long]]] = collection.mutable.Map()
  private val tasksToLaunch: mutable.ListBuffer[String] = mutable.ListBuffer()
  private val listTasks : mutable.Map[String, Task] = mutable.Map()
  var message: String = "Initialization"

  class Task(task_id: String, inputs:mutable.Map[String,Array[Int]]){
    val id: String = task_id
    var state: String = "TASK_STAGING"
    var state_changed: Long = DateTime.now.getMillis
    var reason: String = ""
    var node: String = ""
    var last_node: String = ""
    val description: ormTask = null
    val input:mutable.Map[String,Array[Int]] = inputs


    def update(state: String = state,
               state_changed: Long = state_changed,
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
      val timestamp = new Timestamp(state_changed)
      Map(("id", id), ("state", state),
        ("state-change", state_changed.toString), ("reason", reason),
        ("node", node),("last-node", last_node))
    }
  }


  def newTask(taskId: String, inputs:mutable.Map[String,Array[Int]]) = {
    val task = new Task(taskId, inputs)
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

