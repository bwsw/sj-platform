package com.bwsw.sj.mesos.framework.task.status

import com.bwsw.sj.mesos.framework.task.{TasksList, StatusHandler}
import org.apache.mesos.Protos.TaskStatus

/**
  * Created by diryavkin_dn on 17.11.16.
  */
object FailureHandler extends TaskStatusHandler {
  protected var status: TaskStatus = null

  def setStatus(status: TaskStatus) = {
    this.status = status
    this
  }

  def process() = {
    TasksList(status.getTaskId.getValue).foreach(task => task.update(
      node = "", reason = status.getMessage
    ))
    StatusHandler.logger.error(s"Error: ${status.getMessage}")

    TasksList.addToLaunch(status.getTaskId.getValue)
    StatusHandler.logger.info(s"Added task ${status.getTaskId.getValue} to launch after failure.")
    TasksList.getTask(status.getTaskId.getValue).host = null
  }
}
