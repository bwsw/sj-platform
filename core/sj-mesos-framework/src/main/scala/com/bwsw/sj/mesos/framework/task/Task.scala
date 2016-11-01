package com.bwsw.sj.mesos.framework.task

import java.util.Calendar
import java.util.Date

import com.bwsw.sj.common.DAL.model.module.{Task => InstanceTask}


class Task(taskId: String) {
  val id: String = taskId
  var state: String = "TASK_STAGING"
  var stateChanged: Long = Calendar.getInstance().getTime.getTime
  var reason: String = ""
  var node: String = ""
  var lastNode: String = ""
  val description: InstanceTask = null
  var maxDirectories = 10
  var directories: Array[String] = Array()


  def update(state: String = state,
             stateChanged: Long = stateChanged,
             reason: String = reason,
             node: String = node,
             lastNode: String = lastNode,
             directory: String = "") = {
    this.state = state
    this.stateChanged = stateChanged
    this.reason = reason
    this.node = node
    this.lastNode = lastNode
    if (!directories.contains(directory) && directory.nonEmpty) directories = (directories :+ directory).reverse
    if (directories.length > maxDirectories) directories = directories.reverse.tail.reverse

  }

  def toJson: Map[String, Any] = {
    Map(("id", id),
      ("state", state),
      ("state-change", new Date(stateChanged).toString),
      ("reason", reason),
      ("node", node),
      ("last-node", lastNode),
      ("directories", directories)
    )
  }

}
