package com.bwsw.sj.crud.rest.instance

import java.net.URI

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.TStreamSjStream
import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.bwsw.sj.common.rest.entities.MarathonRequest
import com.bwsw.sj.common.utils.FrameworkLiterals._
import com.bwsw.sj.common.utils.GeneratorLiterals
import com.bwsw.sj.crud.rest.RestLiterals
import org.apache.http.client.methods._
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory

/**
 * Instance methods for starting/stopping/scaling/destroying
 * application on marathon
 *
 * @author Kseniya Tomskikh
 */
trait InstanceMarathonManager {
  private val logger = LoggerFactory.getLogger(getClass.getName)
  private val marathonEntitySerializer = new JsonSerializer(true)
  private lazy val marathonConnect = ConfigurationSettingsUtils.getMarathonConnect()
  private lazy val marathonTimeout = ConfigurationSettingsUtils.getMarathonTimeout()

  def getNumberOfRunningTasks(response: CloseableHttpResponse) = {
    val entity = marathonEntitySerializer.deserialize[MarathonApplicationById](EntityUtils.toString(response.getEntity, "UTF-8"))
    logger.debug(s"Get number of running tasks of application: '${entity.app.id}'.")
    val tasksRunning = entity.app.tasksRunning

    tasksRunning
  }

  def getMarathonMaster(marathonInfo: CloseableHttpResponse) = {
    logger.debug(s"Get a marathon master.")
    val entity = marathonEntitySerializer.deserialize[MarathonInfo](EntityUtils.toString(marathonInfo.getEntity, "UTF-8"))
    val master = entity.marathonConfig.master

    master
  }

  def getLeaderTask(marathonTasks: CloseableHttpResponse): Option[MarathonTask] = {
    //TODO: must get real framework leader!
    val entity = marathonEntitySerializer.deserialize[MarathonApplicationById](EntityUtils.toString(marathonTasks.getEntity, "UTF-8"))
    var leaderTask: Option[MarathonTask] = None
    if (entity.app.tasks != null)
      if (entity.app.tasks.nonEmpty) leaderTask = Some(entity.app.tasks.head)

    leaderTask
  }

  def isStatusOK(response: CloseableHttpResponse) = {
    getStatusCode(response) == 200
  }

  def isStatusCreated(response: CloseableHttpResponse) = {
    getStatusCode(response) == 201
  }

  def isStatusNotFound(response: CloseableHttpResponse) = {
    getStatusCode(response) == 404
  }

  def getStatusCode(response: CloseableHttpResponse) = {
    response.getStatusLine.getStatusCode
  }

  def getFrameworkID(marathonInfo: CloseableHttpResponse) = {
    logger.debug(s"Get a framework id.")
    val entity = marathonEntitySerializer.deserialize[MarathonApplicationById](EntityUtils.toString(marathonInfo.getEntity, "UTF-8"))
    val id = entity.app.env.get(frameworkIdLabel)

    id
  }

  def getGeneratorApplicationID(stream: TStreamSjStream) = {
    var name = ""
    if (stream.generator.generatorType.equals(GeneratorLiterals.perStreamType)) {
      name = s"${stream.generator.service.name}-${stream.name}-${RestLiterals.generatorAppIdSuffix}"
    } else {
      name = s"${stream.generator.service.name}-${RestLiterals.globalGeneratorAppIdSuffix}"
    }
    name.replaceAll("_", "-")
  }

  def getMarathonInfo() = {
    logger.debug(s"Get info about the marathon.")
    val client = new HttpClient(marathonTimeout).client
    val url = new URI(s"$marathonConnect/v2/info")
    val httpGet = new HttpGet(url.toString)
    val marathonInfo = client.execute(httpGet)
    client.close()

    marathonInfo
  }

  def startMarathonApplication(request: MarathonRequest) = {
    logger.debug(s"Start an application on marathon. Request: ${marathonEntitySerializer.serialize(request)}.")
    val client = new HttpClient(marathonTimeout).client
    val url = new URI(s"$marathonConnect/v2/apps")
    val httpPost = new HttpPost(url.toString)
    httpPost.addHeader("Content-Type", "application/json")
    val entity = new StringEntity(marathonEntitySerializer.serialize(request), "UTF-8")
    httpPost.setEntity(entity)
    val marathonInfo = client.execute(httpPost)
    client.close()

    marathonInfo
  }

  def getApplicationInfo(applicationID: String) = {
    logger.debug(s"Get an application info: '$applicationID'.")
    val client = new HttpClient(marathonTimeout).client
    val url = new URI(s"$marathonConnect/v2/apps/$applicationID?force=true")
    val httpGet = new HttpGet(url.toString)
    val marathonInfo = client.execute(httpGet)
    client.close()

    marathonInfo
  }

  def destroyMarathonApplication(applicationID: String) = {
    logger.debug(s"Destroy an application: '$applicationID'.")
    val client = new HttpClient(marathonTimeout).client
    val url = new URI(s"$marathonConnect/v2/apps/$applicationID")
    val httpDelete = new HttpDelete(url.toString)
    val marathonInfo = client.execute(httpDelete)
    client.close()

    marathonInfo
  }

  def stopMarathonApplication(applicationID: String) = {
    logger.debug(s"Stop an application: '$applicationID'.")
    scaleMarathonApplication(applicationID, 0)
  }

  def scaleMarathonApplication(applicationID: String, countOfInstances: Int) = {
    logger.debug(s"Scale an application: '$applicationID' to count: '$countOfInstances'.")
    val client = new HttpClient(marathonTimeout).client
    val url = new URI(s"$marathonConnect/v2/apps/$applicationID?force=true")
    val httpPut = new HttpPut(url.toString)
    httpPut.addHeader("Content-Type", "application/json")
    val entity = new StringEntity(marathonEntitySerializer.serialize(Map("instances" -> countOfInstances)), "UTF-8")
    httpPut.setEntity(entity)
    val marathonInfo = client.execute(httpPut)
    client.close()

    marathonInfo
  }
}
