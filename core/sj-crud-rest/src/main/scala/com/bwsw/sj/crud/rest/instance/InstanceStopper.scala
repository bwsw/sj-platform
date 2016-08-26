package com.bwsw.sj.crud.rest.instance

import com.bwsw.sj.common.DAL.model.module.{InputInstance, Instance}
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
  * One-thread stopper object for instance
  * using synchronous apache http client
  *
  *
  * @author Kseniya Tomskikh
  */
class InstanceStopper(instance: Instance, delay: Long) extends Runnable with InstanceMarathonManager {
  private val logger = LoggerFactory.getLogger(getClass.getName)

  import com.bwsw.sj.common.ModuleConstants._

  def run() = {
    logger.debug(s"Instance: ${instance.name}. Stop instance.")
    try {
      updateInstanceStage(instance, instance.name, stopping)
      val stopResult = stopMarathonApplication(instance.name)
      if (stopResult.getStatusLine.getStatusCode == OK) {
        var isInstanceStopped = false
        while (!isInstanceStopped) {
          val taskInfoResponse = getApplicationInfo(instance.name)
          if (taskInfoResponse.getStatusLine.getStatusCode == OK) {
            val entity = marathonEntitySerializer.deserialize[Map[String, Any]](EntityUtils.toString(taskInfoResponse.getEntity, "UTF-8"))
            val tasksRunning = entity("app").asInstanceOf[Map[String, Any]]("tasksRunning").asInstanceOf[Int]
            if (tasksRunning == 0) {
              instance.status = stopped
              if (instance.moduleType.equals(inputStreamingType)) {
                val tasks = instance.asInstanceOf[InputInstance].tasks.asScala
                instance.asInstanceOf[InputInstance].tasks = mapAsJavaMap(tasks.map { task =>
                  task._2.host = ""
                  task._2.port = 0
                  task
                })
              }
              updateInstanceStage(instance, instance.name, stopped)
              isInstanceStopped = true
            } else {
              updateInstanceStage(instance, instance.name, stopping)
              Thread.sleep(delay)
            }
          } else {
            //todo error?
          }
        }
        logger.debug(s"Instance: ${instance.name}. Instance is stopped.")
      } else {
        logger.debug(s"Instance: ${instance.name}. Instance cannot stopping.")
      }
    } catch {
      case e: Exception =>
    }
  }

}
