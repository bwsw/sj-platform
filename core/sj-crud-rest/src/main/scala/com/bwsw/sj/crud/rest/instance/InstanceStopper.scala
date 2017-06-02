package com.bwsw.sj.crud.rest.instance

import com.bwsw.common.http.HttpClient
import com.bwsw.common.marathon.{MarathonApi, MarathonApplicationById}
import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.bwsw.sj.common.si.model.instance.{InputInstance, Instance}
import com.bwsw.sj.common.utils.EngineLiterals
import org.slf4j.LoggerFactory
import scaldi.Injector

import scala.util.{Failure, Success, Try}
import com.bwsw.common.http.HttpStatusChecker._

/**
  * One-thread stopper object for instance
  * using synchronous apache http client
  *
  * @author Kseniya Tomskikh
  */
class InstanceStopper(instance: Instance, delay: Long = 1000)(implicit val injector: Injector) extends Runnable {

  private val logger = LoggerFactory.getLogger(getClass.getName)
  private val instanceManager = new InstanceDomainRenewer()
  private val marathonTimeout = ConfigurationSettingsUtils.getMarathonTimeout()
  private val client = new HttpClient(marathonTimeout)
  private val marathonManager = new MarathonApi(client)
  private val frameworkName = InstanceAdditionalFieldCreator.getFrameworkName(instance)

  import EngineLiterals._

  def run(): Unit = {
    Try {
      logger.info(s"Instance: '${instance.name}'. Stop an instance.")
      instanceManager.updateInstanceStatus(instance, stopping)
      stopFramework()
      markInstanceAsStopped()
      marathonManager.close()
    } match {
      case Success(_) => logger.info(s"Instance: '${instance.name}' has been stopped.")
      case Failure(e) =>
        logger.error(s"Instance: '${instance.name}'. Instance is failed during the stopping process.", e)
        instanceManager.updateInstanceStatus(instance, error)
        marathonManager.close()
    }
  }

  private def stopFramework() = {
    logger.debug(s"Instance: '${instance.name}'. Stopping a framework.")
    val response = marathonManager.stopMarathonApplication(frameworkName)
    if (isStatusOK(response)) {
      instanceManager.updateFrameworkStage(instance, stopping)
      waitForFrameworkToStop()
    } else {
      instanceManager.updateFrameworkStage(instance, error)
      throw new Exception(s"Marathon returns status code: $response " +
        s"during the stopping process of framework. Framework '$frameworkName' is marked as error.")
    }
  }

  private def waitForFrameworkToStop() = {
    var hasStopped = false
    while (!hasStopped) {
      logger.debug(s"Instance: '${instance.name}'. Waiting until a framework is stopped.")
      val frameworkApplicationInfo = marathonManager.getApplicationInfo(frameworkName)
      if (isStatusOK(frameworkApplicationInfo)) {
        val applicationParsedEntity = marathonManager.getApplicationEntity(frameworkApplicationInfo)

        if (hasFrameworkStopped(applicationParsedEntity)) {
          instanceManager.updateFrameworkStage(instance, stopped)
          hasStopped = true
        } else {
          instanceManager.updateFrameworkStage(instance, stopping)
          Thread.sleep(delay)
        }
      } else {
        instanceManager.updateFrameworkStage(instance, error)
        throw new Exception(s"Marathon returns status code: ${getStatusCode(frameworkApplicationInfo)} " +
          s"during the stopping process of framework. Framework '$frameworkName' is marked as error.")
      }
    }
  }

  private def hasFrameworkStopped(applicationEntity: MarathonApplicationById) = applicationEntity.app.tasksRunning == 0

  private def markInstanceAsStopped() = {
    logger.debug(s"Instance: '${instance.name}'. Mark an instance as stopped.")
    if (isInputInstance) {
      clearTasks()
    }
    instanceManager.updateInstanceStatus(instance, stopped)
    instanceManager.updateInstanceRestAddress(instance, "")
  }

  private def isInputInstance = {
    instance.moduleType.equals(inputStreamingType)
  }

  private def clearTasks() = {
    logger.debug(s"Instance: '${instance.name}'. Clear the input instance tasks.")
    instance.asInstanceOf[InputInstance].tasks.foreach(_._2.clear())
  }
}
