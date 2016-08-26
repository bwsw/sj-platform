package com.bwsw.sj.crud.rest.runner

import java.net.URI
import java.util.Calendar

import com.bwsw.common.JsonSerializer
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.model.{SjStream, TStreamSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.utils.ConfigUtils
import com.bwsw.sj.crud.rest.entities.MarathonRequest
import org.apache.http.client.methods.{HttpDelete, HttpGet, HttpPost, HttpPut}
import org.apache.http.entity.StringEntity
import org.slf4j.LoggerFactory

/**
 * Instance methods for starting/stopping/scaling/destroying
 * application on marathon
 *
 *
 * @author Kseniya Tomskikh
 */
trait InstanceMarathonManager {
  private val logger = LoggerFactory.getLogger(getClass.getName)

  import scala.collection.JavaConversions._

  val marathonEntitySerializer: Serializer = new JsonSerializer
  val instanceDAO: GenericMongoService[Instance] = ConnectionRepository.getInstanceService
  val streamDAO: GenericMongoService[SjStream] = ConnectionRepository.getStreamService

  val OK: Int = 200
  val Created: Int = 201
  val NotFound: Int = 404

  private lazy val restHost = ConfigUtils.getCrudRestHost()
  private lazy val restPort = ConfigUtils.getCrudRestPort()
  private lazy val marathonConnect = ConfigUtils.getMarathonConnect()
  private lazy val marathonTimeout = ConfigUtils.getMarathonTimeout()

  val restAddress = new URI(s"http://$restHost:$restPort").toString

  /**
   *
   * Generating name of task for t-streams stream generator
   *
   * @param stream - SjStream object
   * @return - Task name for transaction generator application
   */
  def getGeneratorAppName(stream: TStreamSjStream) = {
    var name = ""
    if (stream.generator.generatorType.equals("per-stream")) {
      name = s"${stream.generator.service.name}-${stream.name}-tg"
    } else {
      name = s"${stream.generator.service.name}-global-tg"
    }
    name.replaceAll("_", "-")
  }

  def getMarathonInfo = {
    logger.debug(s"Get info about the marathon")
    val client = new HttpClient(marathonTimeout).client
    val url = new URI(s"$marathonConnect/v2/info")
    val httpGet = new HttpGet(url.toString)
    client.execute(httpGet)
  }

  /**
   * Starting application on marathon
   *
   * @param request - Marathon request entity (json)
   * @return - Response from marathon
   */
  def startApplication(request: MarathonRequest) = {
    logger.debug(s"Start application on marathon. Request: ${marathonEntitySerializer.serialize(request)}")
    val client = new HttpClient(marathonTimeout).client
    val url = new URI(s"$marathonConnect/v2/apps")
    val httpPost = new HttpPost(url.toString)
    httpPost.addHeader("Content-Type", "application/json")
    val entity = new StringEntity(marathonEntitySerializer.serialize(request), "UTF-8")
    httpPost.setEntity(entity)
    client.execute(httpPost)
  }

  /**
   * Getting information about application on marathon
   *
   * @param taskId - Application ID
   * @return - Response from marathon
   */
  def getTaskInfo(taskId: String) = {
    logger.debug(s"Getting task info $taskId")
    val client = new HttpClient(marathonTimeout).client
    val url = new URI(s"$marathonConnect/v2/apps/$taskId?force=true")
    val httpGet = new HttpGet(url.toString)
    client.execute(httpGet)
  }

  /**
   * Scale application on marathon
   *
   * @param taskId - Application ID
   * @param count - New count of instances of application
   * @return - Response from marathon
   */
  def scaleApplication(taskId: String, count: Int) = {
    logger.debug(s"Scale task $taskId to count $count")
    val client = new HttpClient(marathonTimeout).client
    val url = new URI(s"$marathonConnect/v2/apps/$taskId?force=true")
    val httpPut = new HttpPut(url.toString)
    httpPut.addHeader("Content-Type", "application/json")
    val entity = new StringEntity(marathonEntitySerializer.serialize(Map("instances" -> count)), "UTF-8")
    httpPut.setEntity(entity)
    client.execute(httpPut)
  }

  /**
   * Destroying application on marathon
   *
   * @param taskId - Application ID
   * @return - Response from marathon
   */
  def destroyApplication(taskId: String) = {
    logger.debug(s"Destroying application $taskId")
    val client = new HttpClient(marathonTimeout).client
    val url = new URI(s"$marathonConnect/v2/apps/$taskId")
    val httpDelete = new HttpDelete(url.toString)
    client.execute(httpDelete)
  }

  /**
   * Stopping application on marathon
   *
   * @param taskId - Generator application ID
   * @return - Response from marathon
   */
  def stopApplication(taskId: String) = {
    logger.debug(s"Stopping application $taskId")
    descaleApplication(taskId)
  }

  /**
   * Suspend application on marathon
   *
   * @param taskId - Application ID
   * @return - Response from marathon
   */
  private def descaleApplication(taskId: String) = {
    logger.debug(s"Descaling application $taskId")
    scaleApplication(taskId, 0)
  }
  
  /**
   * Updating stage of instance
   *
   * @param instance - instance for updating
   * @param stageName - stage name
   * @param state - New (or old) state for stage
   */
  def stageUpdate(instance: Instance, stageName: String, state: String) = {
    logger.debug(s"Update stage $stageName of instance ${instance.name} to state $state")
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
    logger.debug(s"Update stages of instance ${instance.name}")
    instance.stages.keySet().foreach { key =>
      val stage = instance.stages.get(key)
      stageUpdate(instance, key, stage.state)
    }
  }

}
