package com.bwsw.sj.mesos.framework.task

import org.apache.mesos.Protos._

/**
  * Created by diryavkin_dn on 17.11.16.
  */
abstract class Status {
  def process()
}

class FailStatus extends Status {
  override def process() = {
    //do something
  }
}

object StatusBuilder {
  var status: TaskStatus = _
  def setStatus(status: TaskStatus): Unit = {
    this.status = status
  }
  def build: Status =  {
    status.getState.toString match {
      case "TASK_FAILED" => new FailStatus()
    }
  }
}