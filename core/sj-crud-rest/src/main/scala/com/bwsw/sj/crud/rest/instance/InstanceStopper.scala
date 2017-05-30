package com.bwsw.sj.crud.rest.instance

import com.bwsw.sj.common.si.model.instance.{InputInstance, Instance}
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.crud.rest.marathon.MarathonApplicationById
import org.slf4j.LoggerFactory
import scaldi.Injector

import scala.util.{Failure, Success, Try}

/**
  * One-thread stopper object for instance
  * using synchronous apache http client
  *
  * @author Kseniya Tomskikh
  */
class InstanceStopper(instance: Instance, delay: Long = 1000)
                     (override implicit val injector: Injector)
  extends Runnable with InstanceManager {

  private val logger = LoggerFactory.getLogger(getClass.getName)
  private val frameworkName = getFrameworkName(instance)

  import EngineLiterals._

  def run() = {
    Try {
      logger.info(s"Instance: '${instance.name}'. Stop an instance.")
      updateInstanceStatus(instance, stopping)
      stopFramework()
      markInstanceAsStopped()
      close()
    } match {
      case Success(_) => logger.info(s"Instance: '${instance.name}' has been stopped.")
      case Failure(e) =>
        logger.error(s"Instance: '${instance.name}'. Instance is failed during the stopping process.", e)
        updateInstanceStatus(instance, error)
        close()
    }
  }

  private def stopFramework() = {
    logger.debug(s"Instance: '${instance.name}'. Stopping a framework.")
    val response = stopMarathonApplication(frameworkName)
    if (isStatusOK(response)) {
      updateFrameworkStage(instance, stopping)
      waitForFrameworkToStop()
    } else {
      updateFrameworkStage(instance, error)
      throw new Exception(s"Marathon returns status code: $response " +
        s"during the stopping process of framework. Framework '$frameworkName' is marked as error.")
    }
  }

  private def waitForFrameworkToStop() = {
    var hasStopped = false
    while (!hasStopped) {
      logger.debug(s"Instance: '${instance.name}'. Waiting until a framework is stopped.")
      val frameworkApplicationInfo = getApplicationInfo(frameworkName)
      if (isStatusOK(frameworkApplicationInfo)) {
        val applicationParsedEntity = getApplicationEntity(frameworkApplicationInfo)

        if (hasFrameworkStopped(applicationParsedEntity)) {
          updateFrameworkStage(instance, stopped)
          hasStopped = true
        } else {
          updateFrameworkStage(instance, stopping)
          Thread.sleep(delay)
        }
      } else {
        updateFrameworkStage(instance, error)
        throw new Exception(s"Marathon returns status code: ${getStatusCode(frameworkApplicationInfo)} " +
          s"during the stopping process of framework. Framework '$frameworkName' is marked as error.")
      }
    }
  }

  private def hasFrameworkStopped(applicationEntity: MarathonApplicationById) = applicationEntity.app.tasksRunning == 0

  private def markInstanceAsStopped() = {
    logger.debug(s"Instance: '${instance.name}'. Mark an instance as stopped.")
    if (isInputInstance()) {
      clearTasks()
    }
    updateInstanceStatus(instance, stopped)
    updateInstanceRestAddress(instance, "")
  }

  private def isInputInstance() = {
    instance.moduleType.equals(inputStreamingType)
  }

  private def clearTasks() = {
    logger.debug(s"Instance: '${instance.name}'. Clear the input instance tasks.")
    instance.asInstanceOf[InputInstance].tasks.foreach(_._2.clear())
  }
}
