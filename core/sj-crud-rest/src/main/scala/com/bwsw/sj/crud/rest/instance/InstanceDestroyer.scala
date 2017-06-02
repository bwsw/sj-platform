package com.bwsw.sj.crud.rest.instance

import com.bwsw.common.http.HttpClient
import com.bwsw.common.marathon.MarathonApi
import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.utils.EngineLiterals
import org.slf4j.LoggerFactory
import scaldi.Injector

import scala.util.{Failure, Success, Try}
import com.bwsw.common.http.HttpStatusChecker._

/**
  * One-thread deleting object for instance
  * using synchronous apache http client
  *
  * @author Kseniya Tomskikh
  */
class InstanceDestroyer(instance: Instance, delay: Long = 1000)(implicit val injector: Injector) extends Runnable {

  private val logger = LoggerFactory.getLogger(getClass.getName)
  private val instanceManager = new InstanceDomainRenewer()
  private val marathonTimeout = ConfigurationSettingsUtils.getMarathonTimeout()
  private val client = new HttpClient(marathonTimeout)
  private val marathonManager = new MarathonApi(client)
  private val frameworkName = InstanceAdditionalFieldCreator.getFrameworkName(instance)

  import EngineLiterals._

  def run(): Unit = {
    Try {
      logger.info(s"Instance: '${instance.name}'. Destroy an instance.")
      instanceManager.updateInstanceStatus(instance, deleting)
      deleteFramework()
      instanceManager.deleteInstance(instance.name)
      marathonManager.close()
    } match {
      case Success(_) =>
        logger.info(s"Instance: '${instance.name}' has been destroyed.")
      case Failure(e) =>
        logger.error(s"Instance: '${instance.name}'. Instance is failed during the destroying process.", e)
        instanceManager.updateInstanceStatus(instance, error)
        marathonManager.close()
    }
  }

  private def deleteFramework() = {
    logger.debug(s"Instance: '${instance.name}'. Deleting a framework.")
    val response = marathonManager.destroyMarathonApplication(frameworkName)
    if (isStatusOK(response)) {
      instanceManager.updateFrameworkStage(instance, deleting)
      waitForFrameworkToDelete()
    } else {
      if (isStatusNotFound(response)) {
        instanceManager.updateFrameworkStage(instance, deleting)
      } else {
        instanceManager.updateFrameworkStage(instance, error)
        throw new Exception(s"Marathon returns status code: $response " +
          s"during the destroying process of framework. Framework '$frameworkName' is marked as error.")
      }
    }
  }

  private def waitForFrameworkToDelete() = {
    var hasDeleted = false
    while (!hasDeleted) {
      logger.debug(s"Instance: '${instance.name}'. Waiting until a framework is deleted.")
      val frameworkApplicationInfo = marathonManager.getApplicationInfo(frameworkName)
      if (!isStatusNotFound(frameworkApplicationInfo)) {
        instanceManager.updateFrameworkStage(instance, deleting)
        Thread.sleep(delay)
      } else {
        instanceManager.updateFrameworkStage(instance, deleted)
        hasDeleted = true
      }
    } //todo will see about it, maybe get stuck implicitly
  }
}
