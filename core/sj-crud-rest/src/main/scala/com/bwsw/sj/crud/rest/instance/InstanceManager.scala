package com.bwsw.sj.crud.rest.instance

import java.util.Calendar

import com.bwsw.sj.common.DAL.model.TStreamSjStream
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.{GeneratorLiterals, StreamLiterals}

private[instance] trait InstanceManager extends InstanceMarathonManager {
  private val instanceDAO = ConnectionRepository.getInstanceService
  private val streamDAO = ConnectionRepository.getStreamService

  def getFrameworkName(instance: Instance) = instance.name + instance.frameworkId

  protected def hasGeneratorFailed(instance: Instance, name: String) = {
    val stage = instance.stages.get(name)
    stage.state.equals(failed)
  }

  protected def getStreamsHavingGenerator(instance: Instance) = {
    val streams = getInstanceStreams(instance)
    val sjStreamsHavingGenerator = streams.flatMap(streamDAO.get)
      .filter(_.streamType == StreamLiterals.tstreamType)
      .map(_.asInstanceOf[TStreamSjStream])
      .filter(_.generator.generatorType != GeneratorLiterals.localType)

    sjStreamsHavingGenerator
  }

  private def getInstanceStreams(instance: Instance) = {
    var streams = instance.outputs
    if (!instance.moduleType.equals(inputStreamingType)) {
      streams = streams.union(instance.getInputsWithoutStreamMode())
    }

    streams
  }

  protected def updateInstanceStatus(instance: Instance, status: String) = {
    instance.status = status
    instanceDAO.save(instance)
  }

  protected def updateFrameworkState(instance: Instance, status: String) = {
    updateInstanceStage(instance, instance.name, status)
    instanceDAO.save(instance)
  }

  protected def updateGeneratorState(instance: Instance, name: String, status: String) = {
    updateInstanceStage(instance, name, status)
    instanceDAO.save(instance)
  }

  protected def updateInstanceStage(instance: Instance, stageName: String, status: String) = {
    val stage = instance.stages.get(stageName)
    if (stage.state.equals(status)) {
      stage.duration = Calendar.getInstance().getTime.getTime - stage.datetime.getTime
    } else {
      stage.state = status
      stage.datetime = Calendar.getInstance().getTime
      stage.duration = 0
    }
    instance.stages.replace(stageName, stage)
  }
}
