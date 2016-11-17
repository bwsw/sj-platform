package com.bwsw.sj.mesos.framework.task.status

import org.apache.mesos.Protos.TaskStatus

/**
  * Created by diryavkin_dn on 17.11.16.
  */
abstract class TaskStatusHandler {
  protected var status: TaskStatus
  def setStatus(status: TaskStatus): TaskStatusHandler
  def process()
}
