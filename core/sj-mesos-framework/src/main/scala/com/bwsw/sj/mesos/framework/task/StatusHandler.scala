package com.bwsw.sj.mesos.framework.task

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.mesos.framework.schedule.FrameworkUtil
import org.apache.log4j.Logger
import org.apache.mesos.Protos._

/**
  * Handler for mesos task status.
  */
// todo Refactor this class, to use different handler for each status.
object StatusHandler {
  val logger = Logger.getLogger(getClass)
  val serializer = new JsonSerializer()

  /**
    * Determine type of status and restart task, if status "failed" or "error"
    * @param status: mesos task status
    */

  def handle(status: TaskStatus) = {
    logger.debug(s"STATUS UPDATE")

    if (status != null) {

      TasksList(status.getTaskId.getValue).foreach(task => task.update(
        state = status.getState.toString,
        stateChanged = status.getTimestamp.toLong * 1000,
        lastNode = if (task.node != "") task.node else task.lastNode, node = status.getSlaveId.getValue
      ))
      logger.debug(s"Task: ${status.getTaskId.getValue}")
      logger.info(s"Status: ${status.getState}")
      if (status.getState.toString == "TASK_FAILED" ||
        status.getState.toString == "TASK_ERROR") failure_handle(status)
      if (status.getState.toString == "TASK_RUNNING") success_handle(status)
    }
  }


  private def failure_handle(status: TaskStatus) = {
    TasksList(status.getTaskId.getValue).foreach(task => task.update(
      node = "", reason = status.getMessage
    ))
    logger.error(s"Error: ${status.getMessage}")

    TasksList.addToLaunch(status.getTaskId.getValue)
    logger.info(s"Added task ${status.getTaskId.getValue} to launch after failure.")
    TasksList.getTask(status.getTaskId.getValue).host = null
  }

  private def success_handle(status: TaskStatus) = {
    val currentSlave = getCurrentSlave(status.getSlaveId)
    val dir = extractSandbox(currentSlave, status.getTaskId)
    val dirUrl = s"http://${FrameworkUtil.master.getHostname}:${FrameworkUtil.master.getPort}/#/slaves/${currentSlave.id}/browse?path=$dir"

    TasksList(status.getTaskId.getValue).foreach(task => task.update(
      directory = dirUrl
    ))
    logger.debug(s"Running task: ${status.getTaskId.getValue}")

    if (FrameworkUtil.instance.moduleType.equals(EngineLiterals.inputStreamingType)) {
      currentSlave.hostname
    }
  }

  def getCurrentSlave(slaveId: SlaveID) = {
    val masterStateUrl = s"http://${FrameworkUtil.master.getHostname}:${FrameworkUtil.master.getPort}/state.json"
    val masterResponse = scala.io.Source.fromURL(masterStateUrl).mkString
    val masterObj = serializer.deserialize[MasterState](masterResponse)
    masterObj.slaves.filter(slave => slave.id == slaveId.getValue).head
  }

  def extractSandbox(slave:Slave, taskId: TaskID): String = {
    val slaveHost = slave.pid.split("@").last
    val slaveStateUrl = s"http://$slaveHost/state.json"
    val slaveResponse = scala.io.Source.fromURL(slaveStateUrl).mkString
    val obj = serializer.deserialize[SlaveState](slaveResponse)
    val framework = obj.frameworks.filter(framework => framework.id == FrameworkUtil.frameworkId).head
    framework.executors.filter(executor => executor.id == taskId.getValue).head.directory
  }

}
