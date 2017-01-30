package com.bwsw.sj.crud.rest.instance

import com.bwsw.sj.common.DAL.model.module.{InputInstance, Instance}
import com.bwsw.sj.common.utils.EngineLiterals
import org.apache.http.client.methods.CloseableHttpResponse
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
  * One-thread stopper object for instance
  * using synchronous apache http client
  *
  * @author Kseniya Tomskikh
  */
class InstanceStopper(instance: Instance, delay: Long = 1000) extends Runnable with InstanceManager {
  private val logger = LoggerFactory.getLogger(getClass.getName)

  import EngineLiterals._

  def run() = {
    try {
      updateInstanceStatus(instance, stopping)
      stopFramework()
      markInstanceAsStopped()
    } catch {
      case e: Exception =>
        logger.debug(s"Instance: ${instance.name}. Instance is failed during the stopping process.")
        logger.debug(e.getMessage)
        e.printStackTrace()
        updateInstanceStatus(instance, error)
    }
  }

  private def stopFramework() = {
    updateFrameworkState(instance, stopping)
    val response = stopMarathonApplication(instance.name)
    if (isStatusOK(response)) {
      waitForFrameworkToStop()
    } else {
      updateFrameworkState(instance, error)
      throw new Exception(s"Marathon returns status code: ${getStatusCode(response)} " +
        s"during the stopping process of framework. Framework '${instance.name}' is marked as error.")
    }
  }

  private def waitForFrameworkToStop() = {
    var hasStopped = false
    while (!hasStopped) {
      val frameworkApplicationInfo = getApplicationInfo(instance.name)
      if (isStatusOK(frameworkApplicationInfo)) {
        if (hasFrameworkStopped(frameworkApplicationInfo)) {
          updateFrameworkState(instance, stopped)
          hasStopped = true
        } else {
          updateFrameworkState(instance, stopping)
          Thread.sleep(delay)
        }
      } else {
        updateFrameworkState(instance, error)
        throw new Exception(s"Marathon returns status code: ${getStatusCode(frameworkApplicationInfo)} " +
          s"during the stopping process of framework. Framework '${instance.name}' is marked as error.")
      }
    }
  }

  private def hasFrameworkStopped(response: CloseableHttpResponse) = {
    val tasksRunning = getNumberOfRunningTasks(response)

    tasksRunning == 0
  }

  private def markInstanceAsStopped() = {
    if (isInputInstance()) {
      clearTasks()
    }
    updateInstanceStatus(instance, stopped)
  }

  private def isInputInstance() = {
    instance.moduleType.equals(inputStreamingType)
  }

  private def clearTasks() = {
    instance.asInstanceOf[InputInstance].tasks.asScala.foreach(x => x._2.clear())
  }
}
