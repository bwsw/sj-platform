package com.bwsw.sj.crud.rest.instance

import com.bwsw.sj.common.DAL.model.module.{InputInstance, Instance}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
 * One-thread stopper object for instance
 * using synchronous apache http client
 *
 *
 * @author Kseniya Tomskikh
 */
class InstanceStopper(instance: Instance, delay: Long = 1000) extends Runnable with InstanceMarathonManager {
  private val logger = LoggerFactory.getLogger(getClass.getName)
  private val instanceDAO = ConnectionRepository.getInstanceService

  import EngineLiterals._

  def run() = {
    logger.debug(s"Instance: ${instance.name}. Stop instance.")
    try {
      updateInstance(stopping)
      val response = stopMarathonApplication(instance.name)
      if (isStatusOK(response)) {
        waitForInstanceToStop()
        logger.debug(s"Instance: ${instance.name}. Instance has been stopped.")
      } else {
        logger.debug(s"Instance: ${instance.name}. Instance cannot be stopped.")
      }
    } catch {
      case e: Exception => //todo что тут подразумевалось? зачем try catch, если непонятен результат при падении
    }
  }

  private def waitForInstanceToStop() = {
    var hasStopped = false
    while (!hasStopped) {
      val instanceApplicationInfo = getApplicationInfo(instance.name)
      if (isStatusOK(instanceApplicationInfo)) {
        if (hasInstanceStopped(instanceApplicationInfo)) {
          markInstanceAsStopped()
          hasStopped = true
        } else {
          updateInstance(stopping)
          Thread.sleep(delay)
        }
      } else {
        //todo error?
      }
    }
  }

  private def hasInstanceStopped(response: CloseableHttpResponse) = {
    val tasksRunning = getNumberOfRunningTasks(response)

    tasksRunning == 0
  }

  private def getNumberOfRunningTasks(response: CloseableHttpResponse) = {
    val entity = marathonEntitySerializer.deserialize[Map[String, Any]](EntityUtils.toString(response.getEntity, "UTF-8"))
    val tasksRunning = entity("app").asInstanceOf[Map[String, Any]]("tasksRunning").asInstanceOf[Int]

    tasksRunning
  }

  private def markInstanceAsStopped() = {
    if (isInputInstance()) {
      clearTasks()
    }
    updateInstance(stopped)
  }

  private def isInputInstance() = {
    instance.moduleType.equals(inputStreamingType)
  }

  private def clearTasks() = {
    instance.asInstanceOf[InputInstance].tasks.asScala.foreach(x => x._2.clear())
  }

  private def updateInstance(status: String) = {
    updateInstanceStage(instance, instance.name, status)
    updateInstanceStatus(instance, status)
    instanceDAO.save(instance)
  } //todo вынести в отдельный трейт, от которого будут наследоваться starter, destroyer, stopper
}
