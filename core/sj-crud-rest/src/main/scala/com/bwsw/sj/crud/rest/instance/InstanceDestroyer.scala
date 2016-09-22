package com.bwsw.sj.crud.rest.instance

import com.bwsw.sj.common.DAL.model.TStreamSjStream
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.SjStreamUtils._
import com.bwsw.sj.common.utils.{EngineLiterals, GeneratorLiterals, StreamLiterals}
import org.slf4j.LoggerFactory

/**
 * One-thread deleting object for instance
 * using synchronous apache http client
 *
 *
 * @author Kseniya Tomskikh
 */
class InstanceDestroyer(instance: Instance, delay: Long = 1000) extends Runnable with InstanceMarathonManager {
  private val logger = LoggerFactory.getLogger(getClass.getName)
  private val instanceDAO = ConnectionRepository.getInstanceService
  private val streamDAO = ConnectionRepository.getStreamService

  import EngineLiterals._

  def run() = {
    logger.debug(s"Instance: ${instance.name}. Destroy instance.")
    try {
      updateInstance(deleting)
      deleteGenerators()
      deleteInstance()
      logger.debug(s"Instance: ${instance.name}. Instance is deleted.")
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
    val streamsHavingGenerator = getStreamsHavingGenerator()
    val generatorsToDelete = streamsHavingGenerator.filter(isGeneratorAvailableForDeletion).map(x => (x.name, getGeneratorApplicationID(x)))

    generatorsToDelete
  }

  private def getStreamsHavingGenerator() = {
    val streams = getInstanceStreams()
    val sjStreamsHavingGenerator = streams.flatMap(streamDAO.get)
      .filter(_.streamType == StreamLiterals.tStreamType)
      .map(_.asInstanceOf[TStreamSjStream])
      .filter(_.generator.generatorType != GeneratorLiterals.localType)

    sjStreamsHavingGenerator
  }

  private def isGeneratorAvailableForDeletion(stream: TStreamSjStream) = {
    hasGeneratorFailed(stream.name) || !isGeneratorUsed(stream)
  }

  private def hasGeneratorFailed(name: String) = {
    val stage = instance.stages.get(name)
    stage.state.equals(failed)
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
      streams = streams.union(instance.inputs.map(clearStreamFromMode))
    }

    streams
  }

  private def deleteGenerator(streamName: String, applicationID: String) = {
    updateInstance(deleting)
    val taskInfoResponse = getApplicationInfo(applicationID)
    if (!isStatusNotFound(taskInfoResponse)) {
      logger.debug(s"Instance: ${instance.name}. Delete generator: $applicationID.")
      val stopResponse = destroyMarathonApplication(applicationID)
      if (isStatusOK(stopResponse)) {
        waitForGeneratorToDelete(streamName, applicationID)
      }
    } else {
      updateStage(streamName, deleted)
    }
  }

  private def waitForGeneratorToDelete(streamName: String, applicationID: String) = {
    var hasDeleted = false
    while (!hasDeleted) {
      val generatorApplicationInfo = getApplicationInfo(applicationID)
      if (!isStatusNotFound(generatorApplicationInfo)) {
        updateStage(streamName, deleting)
        Thread.sleep(delay)
      } else {
        updateStage(streamName, deleted)
        hasDeleted = true
      }
    }
  }

  private def updateStage(name: String, status: String) = {
    updateInstanceStage(instance, name, status)
    instanceDAO.save(instance)
  }

  private def deleteInstance() = {
    logger.debug(s"Instance: ${instance.name}. Delete instance application.")
    //todo maybe add timeout and retry count?
    val response = destroyMarathonApplication(instance.name)
    if (isStatusOK(response)) {
      waitForInstanceToDelete()
    }
    instanceDAO.delete(instance.name)
  }

  private def waitForInstanceToDelete() = {
    var hasDeleted = false
    while (!hasDeleted) {
      val instanceApplicationInfo = getApplicationInfo(instance.name)
      if (!isStatusNotFound(instanceApplicationInfo)) {
        updateInstance(deleting)
        Thread.sleep(delay)
      } else {
        updateInstance(deleted)
        hasDeleted = true
      }
    }
  }

  private def updateInstance(status: String) = {
    updateInstanceStage(instance, instance.name, status)
    updateInstanceStatus(instance, status)
    instanceDAO.save(instance)
  } //todo вынести в отдельный трейт, от которого будут наследоваться starter, destroyer, stopper
}
