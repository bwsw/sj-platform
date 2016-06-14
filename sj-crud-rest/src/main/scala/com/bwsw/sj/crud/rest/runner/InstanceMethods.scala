package com.bwsw.sj.crud.rest.runner

import java.net.URI
import java.util.Calendar

import com.bwsw.common.JsonSerializer
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.ConfigConstants
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.model.{SjStream, TStreamSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.crud.rest.entities.MarathonRequest
import org.apache.http.client.methods.{HttpDelete, HttpGet, HttpPost, HttpPut}
import org.apache.http.entity.StringEntity

/**
  * Instance methods for starting/stopping/scaling/destroying
  * application on mesos
  * Created: 16/05/2016
  *
  * @author Kseniya Tomskikh
  */
object InstanceMethods {
  import scala.collection.JavaConversions._

  val serializer: Serializer = new JsonSerializer
  val instanceDAO: GenericMongoService[Instance] = ConnectionRepository.getInstanceService
  val streamDAO: GenericMongoService[SjStream] = ConnectionRepository.getStreamService
  val configService = ConnectionRepository.getConfigService

  val OK: Int = 200
  val Created: Int = 201
  val NotFound: Int = 404

  lazy val restHost = configService.get(ConfigConstants.hostOfCrudRestTag).value
  lazy val restPort = configService.get(ConfigConstants.portOfCrudRestTag).value.toInt
  lazy val marathonConnect = configService.get(ConfigConstants.marathonTag).value
  val timeout = 60000

  val restAddress = new URI(s"http://$restHost:$restPort").toString

  /**
    * Generating name of task for t-streams stream generator
    *
    * @param stream - SjStream object
    * @return - Task name for transaction generator application
    */
  def createGeneratorTaskName(stream: TStreamSjStream) = {
    var name = ""
    if (stream.generator.generatorType.equals("per-stream")) {
      name = s"${stream.generator.service.name}-${stream.name}-tg"
    } else {
      name = s"${stream.generator.service.name}-global-tg"
    }
    name.replaceAll("_", "-")
  }

  /**
    * Getting mesos master on Zookeeper from marathon
    *
    * @return - Response from marathon
    */
  def getMesosInfo = {
    val client = new HttpClient(timeout).client
    val url = new URI(s"$marathonConnect/v2/info")
    val httpGet = new HttpGet(url.toString)
    client.execute(httpGet)
  }

  /**
    * Starting application on mesos
    *
    * @param request - Marathon request entity (json)
    * @return - Response from marathon
    */
  def startApplication(request: MarathonRequest) = {
    val client = new HttpClient(timeout).client
    val url = new URI(s"$marathonConnect/v2/apps")
    val httpPost = new HttpPost(url.toString)
    httpPost.addHeader("Content-Type", "application/json")
    val entity = new StringEntity(serializer.serialize(request), "UTF-8")
    httpPost.setEntity(entity)
    client.execute(httpPost)
  }

  /**
    * Getting information about application on mesos
    *
    * @param taskId - Application ID on mesos
    * @return - Response from marathon
    */
  def getTaskInfo(taskId: String) = {
    //logger.debug(s"getting task info $taskId")
    val client = new HttpClient(timeout).client
    val url = new URI(s"$marathonConnect/v2/apps/$taskId?force=true")
    val httpGet = new HttpGet(url.toString)
    client.execute(httpGet)
  }

  /**
    * Scale application on mesos
    *
    * @param taskId - Application ID on marathon for scaling
    * @param count - New count of instances of application
    * @return - Response from marathon
    */
  def scaleApplication(taskId: String, count: Int) = {
    //logger.debug(s"scale task $taskId to count $count")
    val client = new HttpClient(timeout).client
    val url = new URI(s"$marathonConnect/v2/apps/$taskId?force=true")
    val httpPut = new HttpPut(url.toString)
    httpPut.addHeader("Content-Type", "application/json")
    val entity = new StringEntity(serializer.serialize(Map("instances" -> count)), "UTF-8")
    httpPut.setEntity(entity)
    client.execute(httpPut)
  }

  /**
    * Suspend application on mesos
    *
    * @param taskId - Application ID on mesos for suspending
    * @return - Response from marathon
    */
  def descaleApplication(taskId: String) = {
    //logger.debug(s"Descaling application $taskId")
    scaleApplication(taskId, 0)
  }

  /**
    * Destroying application on mesos
    *
    * @param taskId - Application ID on mesos
    * @return - Response from marathon
    */
  def destroyApplication(taskId: String) = {
    val client = new HttpClient(timeout).client
    val url = new URI(s"$marathonConnect/v2/apps/$taskId")
    val httpDelete = new HttpDelete(url.toString)
    client.execute(httpDelete)
  }

  /**
    * Stopping application on mesos
    *
    * @param taskId - Generator task id on mesos
    * @return - Response from marathon
    */
  def stopApplication(taskId: String) = {
    descaleApplication(taskId)
  }

  /**
    * Updating stage of instance
    *
    * @param instance - instance for updating
    * @param stageName - stage name
    * @param state - New (or old) state for stage
    */
  def stageUpdate(instance: Instance, stageName: String, state: String) = {
    val stage = instance.stages.get(stageName)
    if (stage.state.equals(state)) {
      stage.duration = Calendar.getInstance().getTime.getTime - stage.datetime.getTime
    } else {
      stage.state = state
      stage.datetime = Calendar.getInstance().getTime
      stage.duration = 0
    }
    instance.stages.replace(stageName, stage)
    instanceDAO.save(instance)
  }

  /**
    * Updating duration for all stages of instance
    *
    * @param instance - Instance for updating
    */
  def updateInstanceStages(instance: Instance) = {
    instance.stages.keySet().foreach { key =>
      val stage = instance.stages.get(key)
      stageUpdate(instance, key, stage.state)
    }
  }

}
