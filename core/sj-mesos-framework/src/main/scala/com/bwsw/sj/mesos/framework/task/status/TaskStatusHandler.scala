package com.bwsw.sj.mesos.framework.task.status

import org.apache.mesos.Protos.TaskStatus

abstract class TaskStatusHandler {
  protected var status: TaskStatus
  def setStatus(status: TaskStatus): TaskStatusHandler = {
    this.status = status
    this
  }
  def process()
}
