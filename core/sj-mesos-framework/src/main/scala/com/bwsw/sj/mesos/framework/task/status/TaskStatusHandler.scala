package com.bwsw.sj.mesos.framework.task.status

import org.apache.mesos.Protos.TaskStatus

abstract class TaskStatusHandler {
  protected var status: TaskStatus
  //TODO must be implemented and provided in inheritable class
  def setStatus(status: TaskStatus): TaskStatusHandler
  def process()
}
