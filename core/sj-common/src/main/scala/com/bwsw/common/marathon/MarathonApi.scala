package com.bwsw.common.marathon

import java.net.URI

import com.bwsw.common.JsonSerializer
import com.bwsw.common.http.HttpClient
import com.bwsw.common.http.HttpStatusChecker._
import com.bwsw.sj.common.utils.FrameworkLiterals._
import org.apache.http.client.methods._
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory

/**
  * Instance methods provide marathon api
  * in general for starting/stopping/scaling/destroying application on marathon
  *
  * @author Kseniya Tomskikh
  */
class MarathonApi(private val client: HttpClient, marathonAddress: String){
  private val logger = LoggerFactory.getLogger(getClass.getName)
  private val marathonEntitySerializer = new JsonSerializer(true)

  def getApplicationEntity(response: CloseableHttpResponse): MarathonApplication = {
    val entity = marathonEntitySerializer.deserialize[MarathonApplication](EntityUtils.toString(response.getEntity, "UTF-8"))
    logger.debug(s"Get entity of application: '${entity.app.id}'.")

    entity
  }

  def getMarathonMaster(marathonInfo: CloseableHttpResponse): String = {
    logger.debug(s"Get a marathon master.")
    val entity = marathonEntitySerializer.deserialize[MarathonInfo](EntityUtils.toString(marathonInfo.getEntity, "UTF-8"))
    val master = entity.marathonConfig.master

    master
  }

  def getLeaderTask(marathonTasks: CloseableHttpResponse): Option[MarathonTask] = {
    val entity = marathonEntitySerializer.deserialize[MarathonApplication](EntityUtils.toString(marathonTasks.getEntity, "UTF-8"))
    Option(entity.app.tasks) match {
      case Some(l) if l.nonEmpty => Option(entity.app.tasks.head)
      case _ => None
    }
  }

  def getFrameworkID(marathonInfo: CloseableHttpResponse): Option[String] = {
    val entity = marathonEntitySerializer.deserialize[MarathonApplication](EntityUtils.toString(marathonInfo.getEntity, "UTF-8"))
    val id = entity.app.env.get(frameworkIdLabel)

    id
  }

  def getMarathonInfo(): CloseableHttpResponse = {
    logger.debug(s"Get info about the marathon.")
    val url = new URI(s"$marathonAddress/v2/info")
    val httpGet = new HttpGet(url.toString)
    val marathonInfo = client.execute(httpGet)

    marathonInfo
  }

  def startMarathonApplication(request: MarathonRequest): Int = {
    logger.debug(s"Start an application on marathon. Request: ${marathonEntitySerializer.serialize(request)}.")
    val url = new URI(s"$marathonAddress/v2/apps")
    val httpPost = new HttpPost(url.toString)
    httpPost.addHeader("Content-Type", "application/json")
    val entity = new StringEntity(marathonEntitySerializer.serialize(request), "UTF-8")
    httpPost.setEntity(entity)
    val marathonInfo = client.execute(httpPost)
    val statusCode = getStatusCode(marathonInfo)
    discardEntity(marathonInfo)

    statusCode
  }

  def getApplicationInfo(applicationID: String): CloseableHttpResponse = {
    logger.debug(s"Get an application info: '$applicationID'.")
    val url = new URI(s"$marathonAddress/v2/apps/$applicationID?force=true")
    val httpGet = new HttpGet(url.toString)
    val marathonInfo = client.execute(httpGet)

    marathonInfo
  }

  def getMarathonLastTaskFailed(applicationID: String): CloseableHttpResponse = {
    val url = new URI(s"$marathonAddress/v2/apps/$applicationID?embed=lastTaskFailure")
    val httpGet = new HttpGet(url.toString)
    client.execute(httpGet)
  }

  def destroyMarathonApplication(applicationID: String): Int = {
    logger.debug(s"Destroy an application: '$applicationID'.")
    val url = new URI(s"$marathonAddress/v2/apps/$applicationID")
    val httpDelete = new HttpDelete(url.toString)
    val marathonInfo = client.execute(httpDelete)
    val statusCode = getStatusCode(marathonInfo)
    discardEntity(marathonInfo)

    statusCode
  }

  def stopMarathonApplication(applicationID: String): Int = {
    logger.debug(s"Stop an application: '$applicationID'.")
    scaleMarathonApplication(applicationID, 0)
  }

  def scaleMarathonApplication(applicationID: String, countOfInstances: Int): Int = {
    logger.debug(s"Scale an application: '$applicationID' to count: '$countOfInstances'.")
    val url = new URI(s"$marathonAddress/v2/apps/$applicationID?force=true")
    val httpPut = new HttpPut(url.toString)
    httpPut.addHeader("Content-Type", "application/json")
    val entity = new StringEntity(marathonEntitySerializer.serialize(MarathonApplicationInstances(countOfInstances)), "UTF-8")
    httpPut.setEntity(entity)
    val marathonInfo = client.execute(httpPut)
    val statusCode = getStatusCode(marathonInfo)
    discardEntity(marathonInfo)

    statusCode
  }

  private def discardEntity(response: CloseableHttpResponse) = {
    EntityUtils.consumeQuietly(response.getEntity)
  }
}
