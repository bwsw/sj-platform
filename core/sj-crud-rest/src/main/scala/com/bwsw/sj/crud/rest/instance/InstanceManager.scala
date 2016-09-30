package com.bwsw.sj.crud.rest.instance

import java.util.Calendar

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.TStreamSjStream
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.SjStreamUtils._
import com.bwsw.sj.common.utils.{GeneratorLiterals, StreamLiterals}
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.util.EntityUtils

private[instance] trait InstanceManager extends InstanceMarathonManager {
  private val instanceDAO = ConnectionRepository.getInstanceService
  private val streamDAO = ConnectionRepository.getStreamService

  protected def getNumberOfRunningTasks(response: CloseableHttpResponse) = {
    val marathonEntitySerializer = new JsonSerializer
    val entity = marathonEntitySerializer.deserialize[Map[String, Any]](EntityUtils.toString(response.getEntity, "UTF-8"))
    val tasksRunning = entity("app").asInstanceOf[Map[String, Any]]("tasksRunning").asInstanceOf[Int]

    tasksRunning
  }

  protected def hasGeneratorFailed(instance: Instance, name: String) = {
    val stage = instance.stages.get(name)
    stage.state.equals(failed)
  }

  protected def getStreamsHavingGenerator(instance: Instance) = {
    val streams = getInstanceStreams(instance)
    val sjStreamsHavingGenerator = streams.flatMap(streamDAO.get)
      .filter(_.streamType == StreamLiterals.tStreamType)
      .map(_.asInstanceOf[TStreamSjStream])
      .filter(_.generator.generatorType != GeneratorLiterals.localType)

    sjStreamsHavingGenerator
  }

  private def getInstanceStreams(instance: Instance) = {
    var streams = instance.outputs
    if (!instance.moduleType.equals(inputStreamingType)) {
      streams = streams.union(instance.inputs.map(clearStreamFromMode))
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
