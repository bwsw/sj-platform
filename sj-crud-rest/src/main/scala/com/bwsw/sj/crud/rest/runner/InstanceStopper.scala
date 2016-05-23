package com.bwsw.sj.crud.rest.runner

import com.bwsw.sj.common.DAL.model.module.Instance
import org.apache.http.util.EntityUtils

/**
  * One-thread stopper object for instance
  * using synchronous apache http client
  * Created: 16/05/2016
  *
  * @author Kseniya Tomskikh
  */
class InstanceStopper(instance: Instance, delay: Long) extends Runnable {
  import com.bwsw.sj.common.ModuleConstants._
  import InstanceMethods._

  def run() = {
    stageUpdate(instance, instance.name, stopping)
    val stopResult = stopApplication(instance.name)
    if (stopResult.getStatusLine.getStatusCode == OK) {
      var isInstanceStopped = false
      while (!isInstanceStopped) {
        val taskInfoResponse = getTaskInfo(instance.name)
        if (taskInfoResponse.getStatusLine.getStatusCode == OK) {
          val entity = serializer.deserialize[Map[String, Any]](EntityUtils.toString(taskInfoResponse.getEntity, "UTF-8"))
          val tasksRunning = entity("app").asInstanceOf[Map[String, Any]]("tasksRunning").asInstanceOf[Int]
          if (tasksRunning == 0) {
            instance.status = stopped
            stageUpdate(instance, instance.name, stopped)
            isInstanceStopped = true
          } else {
            stageUpdate(instance, instance.name, stopping)
            Thread.sleep(delay)
          }
        } else {
          //todo error?
        }
      }
    } else {
      //todo error?
    }
  }

}
