package com.bwsw.sj.mesos.framework.task

import java.util.{Calendar, Date}

import com.bwsw.sj.common.DAL.model.module.{Task => InstanceTask}
import com.bwsw.sj.common.rest.entities.FrameworkTask

import scala.util.Properties


class Task(taskId: String) {
  val id: String = taskId
  var state: String = "TASK_STAGING"
  var stateChanged: Long = Calendar.getInstance().getTime.getTime
  var reason: String = ""
  var node: String = ""
  var lastNode: String = ""
  //  val description: InstanceTask = null
  var maxDirectories = Properties.envOrElse("MAX_SANDBOX_VIEW", "7").toInt
  var directories: Array[String] = Array()
  var host: Option[String] = None


  def update(state: String = state,
             stateChanged: Long = stateChanged,
             reason: String = reason,
             node: String = node,
             lastNode: String = lastNode,
             directory: String = "",
             host: String = if (this.host.isDefined) this.host.get else null): Unit = {
    this.state = state
    this.stateChanged = stateChanged
    this.reason = reason
    this.node = node
    this.lastNode = lastNode
    this.host = Some(host)
    if (!directories.contains(directory) && directory.nonEmpty) directories = (directories :+ directory).reverse
    if (directories.length > maxDirectories) directories = directories.reverse.tail.reverse

  }

  def toFrameworkTask: FrameworkTask = {
    FrameworkTask(id, state, new Date(stateChanged).toString, reason, node, lastNode, directories)
  }
}
