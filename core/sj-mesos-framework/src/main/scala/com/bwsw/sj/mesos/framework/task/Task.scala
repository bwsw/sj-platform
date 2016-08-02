package com.bwsw.sj.mesos.framework.task

import java.util.Calendar

import com.bwsw.sj.common.DAL.model.module.{Task => InstanceTask}

/**
  * Created: 21/07/2016
  *
  * @author Kseniya Tomskikh
  */
class Task(taskId: String) {
  val id: String = taskId
  var state: String = "TASK_STAGING"
  var stateChanged: Long = Calendar.getInstance().getTime.getTime
  var reason: String = ""
  var node: String = ""
  var lastNode: String = ""
  val description: InstanceTask = null


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
    //val timestamp = new Timestamp(stateChanged)
    Map(("id", id),
      ("state", state),
      ("state-change", stateChanged.toString),
      ("reason", reason),
      ("node", node),
      ("last-node", lastNode)
    )
  }

}
