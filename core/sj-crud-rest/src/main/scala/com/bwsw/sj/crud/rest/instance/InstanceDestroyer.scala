package com.bwsw.sj.crud.rest.instance

import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import org.slf4j.LoggerFactory

/**
 * One-thread deleting object for instance
 * using synchronous apache http client
 *
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
      updateInstanceStatus(instance, deleting)
      deleteFramework()
      deleteInstance()
    } catch {
      case e: Exception =>
        logger.debug(s"Instance: ${instance.name}. Instance is failed during the destroying process.")
        logger.debug(e.getMessage)
        e.printStackTrace()
        updateInstanceStatus(instance, error)
    }
  }

  private def deleteFramework() = {
    val response = destroyMarathonApplication(frameworkName)
    if (isStatusOK(response)) {
      updateFrameworkStage(instance, deleting)
      waitForFrameworkToDelete()
    } else {
      updateFrameworkStage(instance, error)
      throw new Exception(s"Marathon returns status code: ${getStatusCode(response)} " +
        s"during the destroying process of framework. Framework '$frameworkName' is marked as error.")
    }
  }

  private def waitForFrameworkToDelete() = {
    var hasDeleted = false
    while (!hasDeleted) {
      val frameworkApplicationInfo = getApplicationInfo(frameworkName)
      if (!isStatusNotFound(frameworkApplicationInfo)) {
        updateFrameworkStage(instance, deleting)
        Thread.sleep(delay)
      } else {
        updateFrameworkStage(instance, deleted)
        hasDeleted = true
      }
    }     //todo will see about it, maybe get stuck implicitly
  }

  private def deleteInstance() = {
    instanceDAO.delete(instance.name)
  }
}
