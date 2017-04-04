package com.bwsw.sj.mesos.framework.task.status

import com.bwsw.sj.mesos.framework.task.{StatusHandler, TasksList}
import org.apache.mesos.Protos.TaskStatus

/**
  * Created by diryavkin_dn on 06.02.17.
  */
object LostHandler extends TaskStatusHandler {
  protected var status: TaskStatus = null

  def process() = {
    TasksList(status.getTaskId.getValue).foreach(task => task.update(node = "", reason = status.getMessage))
    StatusHandler.logger.error(s"Killed: ${status.getMessage}")

    TasksList.stopped(status.getTaskId.getValue)
    StatusHandler.logger.info(s"Added task ${status.getTaskId.getValue} to launch after failure.")
    TasksList.getTask(status.getTaskId.getValue).host = None
  }
}
