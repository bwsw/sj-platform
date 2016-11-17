package com.bwsw.sj.mesos.framework.task.status

import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.mesos.framework.schedule.FrameworkUtil
import com.bwsw.sj.mesos.framework.task.{MasterState, Slave, SlaveState, TasksList}
import org.apache.mesos.Protos.{SlaveID, TaskID, TaskStatus}
import com.bwsw.sj.mesos.framework.task.StatusHandler

/**
  * Created by diryavkin_dn on 17.11.16.
  */
object SuccessHandler extends TaskStatusHandler {
  protected var status: TaskStatus = null

  def setStatus(status:TaskStatus): TaskStatusHandler = {
    this.status = status
    this
  }

  def process() = {
    val currentSlave = getCurrentSlave(status.getSlaveId)
    val dir = extractSandbox(currentSlave, status.getTaskId)
    val dirUrl = s"http://${FrameworkUtil.master.getHostname}:${FrameworkUtil.master.getPort}/#/slaves/${currentSlave.id}/browse?path=$dir"

    TasksList(status.getTaskId.getValue).foreach(task => task.update(
      directory = dirUrl
    ))
    StatusHandler.logger.debug(s"Running task: ${status.getTaskId.getValue}")

    if (FrameworkUtil.instance.moduleType.equals(EngineLiterals.inputStreamingType)) {
      currentSlave.hostname
    }
  }

  def getCurrentSlave(slaveId: SlaveID) = {
    val masterStateUrl = s"http://${FrameworkUtil.master.getHostname}:${FrameworkUtil.master.getPort}/state.json"
    val masterResponse = scala.io.Source.fromURL(masterStateUrl).mkString
    val masterObj = StatusHandler.serializer.deserialize[MasterState](masterResponse)
    masterObj.slaves.filter(slave => slave.id == slaveId.getValue).head
  }

  def extractSandbox(slave:Slave, taskId: TaskID): String = {
    val slaveHost = slave.pid.split("@").last
    val slaveStateUrl = s"http://$slaveHost/state.json"
    val slaveResponse = scala.io.Source.fromURL(slaveStateUrl).mkString
    val obj = StatusHandler.serializer.deserialize[SlaveState](slaveResponse)
    val framework = obj.frameworks.filter(framework => framework.id == FrameworkUtil.frameworkId).head
    framework.executors.filter(executor => executor.id == taskId.getValue).head.directory
  }
}
