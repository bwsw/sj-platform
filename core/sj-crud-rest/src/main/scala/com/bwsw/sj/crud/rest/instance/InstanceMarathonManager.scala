package com.bwsw.sj.crud.rest.instance

import java.net.URI
import java.util.Calendar

import com.bwsw.common.JsonSerializer
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.model.{SjStream, TStreamSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.utils.{GeneratorLiterals, ConfigSettingsUtils}
import com.bwsw.sj.common.rest.entities.MarathonRequest
import org.apache.http.client.methods.{HttpDelete, HttpGet, HttpPost, HttpPut}
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
  private lazy val restHost = ConfigSettingsUtils.getCrudRestHost()
  private lazy val restPort = ConfigSettingsUtils.getCrudRestPort()
  private lazy val marathonConnect = ConfigSettingsUtils.getMarathonConnect()
  private lazy val marathonTimeout = ConfigSettingsUtils.getMarathonTimeout()

  val marathonEntitySerializer: Serializer = new JsonSerializer
  val instanceDAO: GenericMongoService[Instance] = ConnectionRepository.getInstanceService
  val streamDAO: GenericMongoService[SjStream] = ConnectionRepository.getStreamService
  val OK: Int = 200
  val Created: Int = 201
  val NotFound: Int = 404
  val restAddress = new URI(s"http://$restHost:$restPort").toString

  def getGeneratorAppName(stream: TStreamSjStream) = {
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

  def getApplicationInfo(appId: String) = {
    logger.debug(s"Get application info $appId")
    val client = new HttpClient(marathonTimeout).client
    val url = new URI(s"$marathonConnect/v2/apps/$appId?force=true")
    val httpGet = new HttpGet(url.toString)
    client.execute(httpGet)
  }

  def destroyMarathonApplication(appId: String) = {
    logger.debug(s"Destroy application $appId")
    val client = new HttpClient(marathonTimeout).client
    val url = new URI(s"$marathonConnect/v2/apps/$appId")
    val httpDelete = new HttpDelete(url.toString)
    client.execute(httpDelete)
  }

  def stopMarathonApplication(appId: String) = {
    logger.debug(s"Stop application $appId")
    suspendMarathonApplication(appId)
  }

  private def suspendMarathonApplication(appId: String) = {
    logger.debug(s"Suspend application $appId")
    scaleMarathonApplication(appId, 0)
  }

  def scaleMarathonApplication(appId: String, countOfInstances: Int) = {
    logger.debug(s"Scale application $appId to count $countOfInstances")
    val client = new HttpClient(marathonTimeout).client
    val url = new URI(s"$marathonConnect/v2/apps/$appId?force=true")
    val httpPut = new HttpPut(url.toString)
    httpPut.addHeader("Content-Type", "application/json")
    val entity = new StringEntity(marathonEntitySerializer.serialize(Map("instances" -> countOfInstances)), "UTF-8")
    httpPut.setEntity(entity)
    client.execute(httpPut)
  }

  def updateInstanceStage(instance: Instance, stageName: String, state: String) = {
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
}
