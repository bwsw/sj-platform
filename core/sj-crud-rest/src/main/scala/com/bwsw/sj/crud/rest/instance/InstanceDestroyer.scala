package com.bwsw.sj.crud.rest.instance

import com.bwsw.sj.common.dal.model.module.Instance
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import org.slf4j.LoggerFactory

/**
  * One-thread deleting object for instance
  * using synchronous apache http client
  *
  * @author Kseniya Tomskikh
  */
class InstanceDestroyer(instance: Instance, delay: Long = 1000) extends Runnable with InstanceManager {
  private val logger = LoggerFactory.getLogger(getClass.getName)
  private val instanceDAO = ConnectionRepository.getInstanceService
  private val frameworkName = getFrameworkName(instance)

  import EngineLiterals._

  def run() = {
    try {
      logger.info(s"Instance: '${instance.name}'. Destroy an instance.")
      updateInstanceStatus(instance, deleting)
      deleteFramework()
      deleteInstance()
      close()
      logger.info(s"Instance: '${instance.name}' has been destroyed.")
    } catch {
      case e: Exception =>
        logger.error(s"Instance: '${instance.name}'. Instance is failed during the destroying process.", e)
        updateInstanceStatus(instance, error)
        close()
    }
  }

  private def deleteFramework() = {
    logger.debug(s"Instance: '${instance.name}'. Deleting a framework.")
    val response = destroyMarathonApplication(frameworkName)
    if (isStatusOK(response)) {
      updateFrameworkStage(instance, deleting)
      waitForFrameworkToDelete()
    } else {
      if (isStatusNotFound(response)) {
        updateFrameworkStage(instance, deleting)
      } else {
        updateFrameworkStage(instance, error)
        throw new Exception(s"Marathon returns status code: $response " +
          s"during the destroying process of framework. Framework '$frameworkName' is marked as error.")
      }
    }
  }

  private def waitForFrameworkToDelete() = {
    var hasDeleted = false
    while (!hasDeleted) {
      logger.debug(s"Instance: '${instance.name}'. Waiting until a framework is deleted.")
      val frameworkApplicationInfo = getApplicationInfo(frameworkName)
      if (!isStatusNotFound(frameworkApplicationInfo)) {
        updateFrameworkStage(instance, deleting)
        Thread.sleep(delay)
      } else {
        updateFrameworkStage(instance, deleted)
        hasDeleted = true
      }
    } //todo will see about it, maybe get stuck implicitly
  }

  private def deleteInstance() = {
    instanceDAO.delete(instance.name)
  }
}
