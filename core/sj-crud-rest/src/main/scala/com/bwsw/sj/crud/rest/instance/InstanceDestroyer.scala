package com.bwsw.sj.crud.rest.instance

import com.bwsw.sj.common.DAL.model.TStreamSjStream
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

  import EngineLiterals._

  def run() = {
    try {
      updateInstanceStatus(instance, deleting)
      deleteGenerators()
      deleteFramework()
      deleteInstance()
    } catch {
      case e: Exception => //todo что тут подразумевалось? зачем try catch, если непонятен результат при падении
    }
  }

  private def deleteGenerators() = {
    logger.debug(s"Instance: ${instance.name}. Deleting generators.")
    val generators = getGeneratorsToDelete()
    generators.foreach(x => deleteGenerator(x._1, x._2))
  }

  private def getGeneratorsToDelete() = {
    val streamsHavingGenerator = getStreamsHavingGenerator(instance)
    val generatorsToDelete = streamsHavingGenerator.filter(isGeneratorAvailableForDeletion).map(x => (x.name, getGeneratorApplicationID(x)))

    generatorsToDelete
  }

  private def isGeneratorAvailableForDeletion(stream: TStreamSjStream) = {
    hasGeneratorFailed(instance, stream.name) || !isGeneratorUsed(stream)
  }

  private def isGeneratorUsed(stream: TStreamSjStream) = {
    val startedInstances = instanceDAO.getByParameters(Map("status" -> started))
    val startingInstances = instanceDAO.getByParameters(Map("status" -> starting))
    startedInstances.exists(instance => doesInstanceContainStream(instance, stream.name)) ||
      startingInstances.exists(instance => doesInstanceContainStream(instance, stream.name))
  }

  private def doesInstanceContainStream(instance: Instance, name: String) = {
    val streams = getInstanceStreams()

    streams.contains(name)
  }

  private def getInstanceStreams() = {
    var streams = instance.outputs
    if (!instance.moduleType.equals(inputStreamingType)) {
      streams = streams.union(instance.getInputsWithoutStreamMode())
    }

    streams
  }

  private def deleteGenerator(streamName: String, applicationID: String) = {
    updateGeneratorState(instance, streamName, deleting)
    val response = destroyMarathonApplication(applicationID)
    if (isStatusOK(response)) {
      waitForGeneratorToDelete(streamName, applicationID)
    }
  }

  private def waitForGeneratorToDelete(streamName: String, applicationID: String) = {
    var hasDeleted = false
    while (!hasDeleted) {
      val generatorApplicationInfo = getApplicationInfo(applicationID)
      if (!isStatusNotFound(generatorApplicationInfo)) {
        updateGeneratorState(instance, streamName, deleting)
        Thread.sleep(delay)
      } else {
        updateGeneratorState(instance, streamName, deleted)
        hasDeleted = true
      }
    }
  }

  private def deleteFramework() = {
    updateFrameworkState(instance, deleting)
    val response = destroyMarathonApplication(instance.name)
    if (isStatusOK(response)) {
      waitForFrameworkToDelete()
    }
  }

  private def waitForFrameworkToDelete() = {
    var hasDeleted = false
    while (!hasDeleted) {
      val frameworkApplicationInfo = getApplicationInfo(instance.name)
      if (!isStatusNotFound(frameworkApplicationInfo)) {
        updateFrameworkState(instance, deleting)
        Thread.sleep(delay)
      } else {
        updateFrameworkState(instance, deleted)
        hasDeleted = true
      }
    }
  }

  private def deleteInstance() = {
    instanceDAO.delete(instance.name)
  }
}
