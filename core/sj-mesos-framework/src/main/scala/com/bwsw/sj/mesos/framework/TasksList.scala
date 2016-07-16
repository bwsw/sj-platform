package com.bwsw.sj.mesos.framework

import com.bwsw.sj.common.DAL.model.module.{Task => ormTask}
import scala.collection.mutable
import java.sql.Timestamp
import org.joda.time.DateTime

/**
  * Created by diryavkin_dn on 18.05.16.
  */
object TasksList {

  private val tasksToLaunch: mutable.ListBuffer[String] = mutable.ListBuffer()
  private val listTasks : mutable.Map[String, Task] = mutable.Map()
  var message: String = "Initialization"

  class Task(taskId: String) {
    val id: String = taskId
    var state: String = "TASK_STAGING"
    var stateChanged: Long = DateTime.now.getMillis
    var reason: String = ""
    var node: String = ""
    var lastNode: String = ""
    val description: ormTask = null


    def update(state: String = state,
               stateChanged: Long = stateChanged,
               reason: String = reason,
               node: String = node,
               lastNode: String = lastNode) = {
      this.state = state
      this.stateChanged = stateChanged
      this.reason = reason
      this.node = node
      this.lastNode = lastNode
    }

    def toJson: Map[String, Any] = {
      val timestamp = new Timestamp(stateChanged)
      Map(("id", id),
        ("state", state),
        ("state-change", stateChanged.toString),
        ("reason", reason),
        ("node", node),
        ("last-node", lastNode)
      )
    }

  }

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
    Map(("tasks", listTasks.values.map{x => x.toJson}),
      ("message", message))
  }

  def apply(taskId: String): Option[Task] = {
    listTasks.get(taskId)
  }
}

