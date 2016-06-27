package com.bwsw.sj.crud.rest.runner

import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.model.{SjStream, TStreamSjStream}
import com.bwsw.sj.common.StreamConstants
import org.slf4j.LoggerFactory

/**
  * One-thread deleting object for instance
  * using synchronous apache http client
  * Created: 16/05/2016
  *
  * @author Kseniya Tomskikh
  */
class InstanceDestroyer(instance: Instance, delay: Long) extends Runnable {
  private val logger = LoggerFactory.getLogger(getClass.getName)

  import InstanceMethods._
  import com.bwsw.sj.common.ModuleConstants._

  def run() = {
    logger.debug(s"Instance: ${instance.name}. Destroy instance.")
    stageUpdate(instance, instance.name, deleting)
    deleteGenerators(instance)
    deleteInstance(instance)
    instanceDAO.delete(instance.name)
    logger.debug(s"Instance: ${instance.name}. Instance is deleted.")
  }

  /**
    * Stopping all running generators for streams of instance,
    * if generators is not using any streams of started instances
    *
    * @param instance - Instance for stopping
    * @return - Response from marathon
    */
  def deleteGenerators(instance: Instance) = {
    logger.debug(s"Instance: ${instance.name}. Deleting generators.")
    val allStreams = instance.inputs.map(_.replaceAll("/split|/full", "")).union(instance.outputs).map(streamDAO.get)
    val startedInstances = instanceDAO.getByParameters(Map("status" -> started))
    val startingInstance = startedInstances.union(instanceDAO.getByParameters(Map("status" -> starting)))
    val tStreamStreamsToStop = allStreams
      .filter { stream: SjStream =>
        if (stream.streamType.equals(StreamConstants.tStream)) {
          val stage = instance.stages.get(stream.name)
          !stream.asInstanceOf[TStreamSjStream].generator.generatorType.equals("local") &&
            (stage.state.equals(failed) ||
              !startingInstance.exists { instance =>
                val streamGeneratorName = createGeneratorTaskName(stream.asInstanceOf[TStreamSjStream])
                val instanceStreamGenerators = instance.inputs.map(_.replaceAll("/split|/full", ""))
                  .union(instance.outputs)
                  .map(name => streamDAO.get(name))
                  .filter(s => s.streamType.equals(StreamConstants.tStream))
                  .map(sjStream => createGeneratorTaskName(sjStream.asInstanceOf[TStreamSjStream]))
                instanceStreamGenerators.contains(streamGeneratorName)
              })
        } else false
    }
    tStreamStreamsToStop.foreach { stream =>
      var isTaskDeleted = false
      stageUpdate(instance, stream.name, deleting)
      val taskId = createGeneratorTaskName(stream.asInstanceOf[TStreamSjStream])
      val taskInfoResponse = getTaskInfo(taskId)
      if (taskInfoResponse.getStatusLine.getStatusCode != NotFound) {
        logger.debug(s"Instance: ${instance.name}. Delete generator: $taskId.")
        val stopResponse = destroyApplication(taskId)
        if (stopResponse.getStatusLine.getStatusCode == OK) {
          while (!isTaskDeleted) {
            val taskInfoResponse = getTaskInfo(taskId)
            if (taskInfoResponse.getStatusLine.getStatusCode != NotFound) {
              stageUpdate(instance, stream.name, deleting)
              Thread.sleep(delay)
            } else {
              stageUpdate(instance, stream.name, deleted)
              isTaskDeleted = true
            }
          }
        } else {
          //todo what doing?
        }
      } else {
        stageUpdate(instance, stream.name, deleted)
        isTaskDeleted = true
      }
    }
  }

  /**
    * Destroying application of instance on mesos
    *
    * @param instance - Instance for deleting
    */
  def deleteInstance(instance: Instance) = {
    logger.debug(s"Instance: ${instance.name}. Delete instance application.")
    var isInstanceDeleted = false
    //todo maybe add timeout and retry count?
    while (!isInstanceDeleted) {
      val response = destroyApplication(instance.name)
      if (response.getStatusLine.getStatusCode == OK) {
        while (!isInstanceDeleted) {
          val taskInfoResponse = getTaskInfo(instance.name)
          if (taskInfoResponse.getStatusLine.getStatusCode != NotFound) {
            stageUpdate(instance, instance.name, deleting)
            Thread.sleep(delay)
          } else {
            instance.status = deleted
            stageUpdate(instance, instance.name, deleted)
            isInstanceDeleted = true
          }
        }
      } else {
        //todo error?
      }
    }
  }

}
