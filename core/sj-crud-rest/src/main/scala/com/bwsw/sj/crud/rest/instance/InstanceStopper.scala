package com.bwsw.sj.crud.rest.instance

import com.bwsw.common.http.HttpClient
import com.bwsw.common.http.HttpStatusChecker._
import com.bwsw.common.marathon.{MarathonApi, MarathonApplication}
import com.bwsw.sj.common.si.model.instance.{InputInstance, Instance}
import com.bwsw.sj.common.utils.EngineLiterals
import org.slf4j.LoggerFactory
import scaldi.Injector

import scala.util.{Failure, Success, Try}

/**
  * One-thread stopper object for instance
  * using synchronous apache http client
  *
  * protected methods and variables need for testing purposes
  *
  * @author Kseniya Tomskikh
  */
class InstanceStopper(instance: Instance, marathonAddress: String, delay: Long = 1000, marathonTimeout: Int = 60000)(implicit val injector: Injector) extends Runnable {

  private val logger = LoggerFactory.getLogger(getClass.getName)
  protected val instanceManager = new InstanceDomainRenewer()
  protected val client = new HttpClient(marathonTimeout)
  protected val marathonManager = new MarathonApi(client, marathonAddress)
  private val frameworkName = InstanceAdditionalFieldCreator.getFrameworkName(instance)

  import EngineLiterals._

  def run(): Unit = {
    Try {
      logger.info(s"Instance: '${instance.name}'. Stop an instance.")
      instanceManager.updateInstanceStatus(instance, stopping)
      stopFramework()
      markInstanceAsStopped()
      client.close()
    } match {
      case Success(_) => logger.info(s"Instance: '${instance.name}' has been stopped.")
      case Failure(e) =>
        logger.error(s"Instance: '${instance.name}'. Instance is failed during the stopping process.", e)
        instanceManager.updateInstanceStatus(instance, error)
        instanceManager.updateInstanceRestAddress(instance, None)
        client.close()
    }
  }

  protected def stopFramework(): Unit = {
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

  protected def waitForFrameworkToStop(): Unit = {
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

  private def hasFrameworkStopped(applicationEntity: MarathonApplication): Boolean = applicationEntity.app.tasksRunning == 0

  protected def markInstanceAsStopped(): Unit = {
    logger.debug(s"Instance: '${instance.name}'. Mark an instance as stopped.")
    if (isInputInstance) {
      clearTasks()
    }
    instanceManager.updateInstanceStatus(instance, stopped)
    instanceManager.updateInstanceRestAddress(instance, None)
  }

  private def isInputInstance: Boolean = {
    instance.moduleType.equals(inputStreamingType)
  }

  private def clearTasks(): Unit = {
    logger.debug(s"Instance: '${instance.name}'. Clear the input instance tasks.")
    instance.asInstanceOf[InputInstance].tasks.foreach(_._2.clear())
  }
}
