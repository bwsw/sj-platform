package com.bwsw.sj.mesos.framework.task.status

import com.bwsw.sj.mesos.framework.task.{TasksList, StatusHandler}
import org.apache.mesos.Protos.TaskStatus

object FailureHandler {

  def process(status: TaskStatus) = {
    TasksList(status.getTaskId.getValue).foreach(task => task.update(node = "", reason = status.getMessage))
    StatusHandler.logger.error(s"Error: ${status.getMessage}")

    TasksList.stopped(status.getTaskId.getValue)
    StatusHandler.logger.info(s"Added task ${status.getTaskId.getValue} to launch after failure.")
    TasksList.getTask(status.getTaskId.getValue).host = None
  }
}
