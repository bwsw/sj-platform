package com.bwsw.sj.crud.rest.instance

import java.net.URI
import java.util.Calendar

import com.bwsw.common.JsonSerializer
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.DAL.model.TStreamSjStream
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.rest.entities.MarathonRequest
import com.bwsw.sj.common.utils.{ConfigSettingsUtils, GeneratorLiterals}
import org.apache.http.client.methods._
import org.apache.http.entity.StringEntity
import org.slf4j.LoggerFactory

/**
 * Instance methods for starting/stopping/scaling/destroying
 * application on marathon
 *
 * @author Kseniya Tomskikh
 */
trait InstanceMarathonManager {
  private val logger = LoggerFactory.getLogger(getClass.getName)
  private lazy val marathonConnect = ConfigSettingsUtils.getMarathonConnect()
  private lazy val marathonTimeout = ConfigSettingsUtils.getMarathonTimeout()

  val marathonEntitySerializer: Serializer = new JsonSerializer

  def isStatusOK(response: CloseableHttpResponse) = {
    response.getStatusLine.getStatusCode == 200
  }

  def isStatusCreated(response: CloseableHttpResponse) = {
    response.getStatusLine.getStatusCode == 201
  }

  def isStatusNotFound(response: CloseableHttpResponse) = {
    response.getStatusLine.getStatusCode == 404
  }

  def getGeneratorApplicationID(stream: TStreamSjStream) = {
    var name = ""
    if (stream.generator.generatorType.equals(GeneratorLiterals.perStreamType)) {
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

  def startMarathonApplication(request: MarathonRequest) = {
    logger.debug(s"Start application on marathon. Request: ${marathonEntitySerializer.serialize(request)}")
    val client = new HttpClient(marathonTimeout).client
    val url = new URI(s"$marathonConnect/v2/apps")
    val httpPost = new HttpPost(url.toString)
    httpPost.addHeader("Content-Type", "application/json")
    val entity = new StringEntity(marathonEntitySerializer.serialize(request), "UTF-8")
    httpPost.setEntity(entity)
    client.execute(httpPost)
  }

  def getApplicationInfo(applicationID: String) = {
    logger.debug(s"Get application info $applicationID")
    val client = new HttpClient(marathonTimeout).client
    val url = new URI(s"$marathonConnect/v2/apps/$applicationID?force=true")
    val httpGet = new HttpGet(url.toString)
    client.execute(httpGet)
  }

  def destroyMarathonApplication(applicationID: String) = {
    logger.debug(s"Destroy application $applicationID")
    val client = new HttpClient(marathonTimeout).client
    val url = new URI(s"$marathonConnect/v2/apps/$applicationID")
    val httpDelete = new HttpDelete(url.toString)
    client.execute(httpDelete)
  }

  def stopMarathonApplication(applicationID: String) = {
    logger.debug(s"Stop application $applicationID")
    suspendMarathonApplication(applicationID)
  }

  private def suspendMarathonApplication(applicationID: String) = {
    logger.debug(s"Suspend application $applicationID")
    scaleMarathonApplication(applicationID, 0)
  }

  def scaleMarathonApplication(applicationID: String, countOfInstances: Int) = {
    logger.debug(s"Scale application $applicationID to count $countOfInstances")
    val client = new HttpClient(marathonTimeout).client
    val url = new URI(s"$marathonConnect/v2/apps/$applicationID?force=true")
    val httpPut = new HttpPut(url.toString)
    httpPut.addHeader("Content-Type", "application/json")
    val entity = new StringEntity(marathonEntitySerializer.serialize(Map("instances" -> countOfInstances)), "UTF-8")
    httpPut.setEntity(entity)
    client.execute(httpPut)
  }

  def updateInstanceStatus(instance: Instance, status: String) = {
    instance.status = status
  } //todo вынести в отдельный трейт, от которого будут наследоваться starter, destroyer, stopper

  def updateInstanceStage(instance: Instance, stageName: String, status: String) = {
    logger.debug(s"Update stage $stageName of instance ${instance.name} to state $status")
    val stage = instance.stages.get(stageName)
    if (stage.state.equals(status)) {
      stage.duration = Calendar.getInstance().getTime.getTime - stage.datetime.getTime
    } else {
      stage.state = status
      stage.datetime = Calendar.getInstance().getTime
      stage.duration = 0
    }
    instance.stages.replace(stageName, stage)
  } //todo вынести в отдельный трейт, от которого будут наследоваться starter, destroyer, stopper
}
