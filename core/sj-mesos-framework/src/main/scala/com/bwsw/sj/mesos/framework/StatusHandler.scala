package com.bwsw.sj.mesos.framework

import org.apache.log4j.Logger
import org.apache.mesos.Protos._

/**
  * Created by diryavkin_dn on 10.06.16.
  */


object StatusHandler {
  val logger = Logger.getLogger(getClass)

  def handle(status: TaskStatus) = {
    logger.info(s"STATUS UPDATE")

    if (status != null) {

      TasksList(status.getTaskId.getValue).foreach(task => task.update(
        state = status.getState.toString,
        stateChanged = status.getTimestamp.toLong * 1000,
        lastNode = if (task.node != "") task.node else task.lastNode, node = status.getSlaveId.getValue
      ))
      logger.info(s"Task: ${status.getTaskId.getValue}")
      logger.info(s"Status: ${status.getState}")
      if (status.getState.toString == "TASK_FAILED" || status.getState.toString == "TASK_ERROR") {
        TasksList(status.getTaskId.getValue).foreach(x => x.update(
          node = "", reason = status.getMessage
        ))
        logger.info(s"Error: ${status.getMessage}")

        TasksList.addToLaunch(status.getTaskId.getValue)
        logger.info("Added task to launch")
      }
    }
  }
}
