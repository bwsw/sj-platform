package com.bwsw.sj.crud.rest.instance

import java.net.URI

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.TStreamSjStream
import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.bwsw.sj.common.rest.entities.MarathonRequest
import com.bwsw.sj.common.utils.GeneratorLiterals
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
  private val marathonEntitySerializer = new JsonSerializer
  private lazy val marathonConnect = ConfigurationSettingsUtils.getMarathonConnect()
  private lazy val marathonTimeout = ConfigurationSettingsUtils.getMarathonTimeout()

  def getMarathonMaster(marathonInfo: CloseableHttpResponse) = {
    val entity = marathonEntitySerializer.deserialize[Map[String, Any]](EntityUtils.toString(marathonInfo.getEntity, "UTF-8"))
    val master = entity.get("marathon_config").get.asInstanceOf[Map[String, Any]].get("master").get.asInstanceOf[String]

    master
  }

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

  def getMarathonInfo() = {
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
}
