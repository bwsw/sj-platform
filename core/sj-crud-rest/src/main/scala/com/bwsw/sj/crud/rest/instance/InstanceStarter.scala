package com.bwsw.sj.crud.rest.instance

import java.net.URI

import com.bwsw.common.LeaderLatch
import com.bwsw.sj.common.DAL.ConnectionConstants
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.bwsw.sj.common.rest.entities.MarathonRequest
import com.bwsw.sj.common.utils.FrameworkLiterals._
import com.bwsw.sj.common.utils._
import com.bwsw.sj.crud.rest.RestLiterals
import org.apache.http.client.methods.CloseableHttpResponse
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
  * One-thread starting object for instance
  * using synchronous apache http client
  *
  * @author Kseniya Tomskikh
  */
class InstanceStarter(instance: Instance, delay: Long = 1000) extends Runnable with InstanceManager {

  import EngineLiterals._

  private val logger = LoggerFactory.getLogger(getClass.getName)
  private lazy val restHost = ConfigurationSettingsUtils.getCrudRestHost()
  private lazy val restPort = ConfigurationSettingsUtils.getCrudRestPort()
  private lazy val restAddress = new URI(s"http://$restHost:$restPort").toString
  private val frameworkName = getFrameworkName(instance)

  def run() = {
    logger.debug(s"Instance: ${instance.name}. Start instance.")
    try {
      updateInstanceStatus(instance, starting)
      startInstance()
    } catch {
      case e: Exception =>
        logger.debug(s"Instance: ${instance.name}. Instance is failed during the start process.")
        logger.debug(e.getMessage)
        e.printStackTrace()
        updateInstanceStatus(instance, failed)
    }
  }

  private def startInstance() = {
    val marathonInfo = getMarathonInfo()
    if (isStatusOK(marathonInfo)) {
      val marathonMaster = getMarathonMaster(marathonInfo)
      val leaderLatch = createLeaderLatch(marathonMaster)
      updateFrameworkStage(instance, starting)
      startFramework(marathonMaster)
      leaderLatch.close()
    } else {
      updateInstanceStatus(instance, failed)
    }
  }

  private def createLeaderLatch(marathonMaster: String) = {
    val zkServers = getZooKeeperServers(marathonMaster)
    val leader = new LeaderLatch(Set(zkServers), RestLiterals.masterNode)
    leader.start()
    leader.takeLeadership(delay)

    leader
  }

  private def getZooKeeperServers(marathonMaster: String) = {
    var zooKeeperServers = ""
    val zkHost = System.getenv("ZOOKEEPER_HOST")
    val zkPort = System.getenv("ZOOKEEPER_PORT")
    if (zkHost != null && zkPort != null) {
      zooKeeperServers = zkHost + ":" + zkPort
    } else {
      val marathonMasterUrl = new URI(marathonMaster)
      zooKeeperServers = marathonMasterUrl.getHost + ":" + marathonMasterUrl.getPort
    }

    zooKeeperServers
  }

  private def startFramework(marathonMaster: String) = {
    val frameworkApplicationInfo = getApplicationInfo(frameworkName)
    if (isStatusOK(frameworkApplicationInfo)) {
      launchFramework()
    } else {
      createFramework(marathonMaster)
    }
  }

  private def launchFramework() = {
    val startFrameworkResult = scaleMarathonApplication(frameworkName, 1)
    if (isStatusOK(startFrameworkResult)) {
      waitForFrameworkToStart()
    } else {
      updateFrameworkStage(instance, failed)
      updateInstanceStatus(instance, failed)
      updateInstanceRestAddress(instance, "")
    }
  }

  private def createFramework(marathonMaster: String) = {
    val request = createRequestForFrameworkCreation(marathonMaster)
    val startFrameworkResult = startMarathonApplication(request)
    if (isStatusCreated(startFrameworkResult)) {
      waitForFrameworkToStart()
    } else {
      updateFrameworkStage(instance, failed)
      updateInstanceStatus(instance, failed)
      updateInstanceRestAddress(instance, "")
    }
  }

  private def createRequestForFrameworkCreation(marathonMaster: String) = {
    val frameworkJarName = ConfigurationSettingsUtils.getFrameworkJarName()
    val command = "java -jar " + frameworkJarName + " $PORT"
    val restUrl = new URI(s"$restAddress/v1/custom/jars/$frameworkJarName")
    val environmentVariables = getFrameworkEnvironmentVariables(marathonMaster)
    val backoffSettings = getBackoffSettings()
    val request = MarathonRequest(
      frameworkName,
      command,
      1,
      environmentVariables,
      List(restUrl.toString),
      backoffSettings._1,
      backoffSettings._2,
      backoffSettings._3)

    request
  }

  private def getFrameworkEnvironmentVariables(marathonMaster: String) = {
    var environmentVariables = Map(
      instanceIdLabel -> instance.name,
      mesosMasterLabel -> marathonMaster
    )
    environmentVariables = environmentVariables ++ ConnectionConstants.mongoEnvironment
    environmentVariables = environmentVariables ++ instance.environmentVariables.asScala

    environmentVariables
  }

  private def getBackoffSettings(): (Int, Double, Int) = {
    val backoffSeconds = try ConfigurationSettingsUtils.getFrameworkBackoffSeconds() catch {
      case e: NoSuchFieldException => 7
    }
    val backoffFactor = try ConfigurationSettingsUtils.getFrameworkBackoffFactor() catch {
      case e: NoSuchFieldException => 7
    }
    val maxLaunchDelaySeconds = try ConfigurationSettingsUtils.getFrameworkMaxLaunchDelaySeconds() catch {
      case e: NoSuchFieldException => 600
    }
    (backoffSeconds, backoffFactor, maxLaunchDelaySeconds)
  }

  private def waitForFrameworkToStart() = {
    var isStarted = false
    while (!isStarted) {
      val frameworkApplicationInfo = getApplicationInfo(frameworkName)
      if (isStatusOK(frameworkApplicationInfo)) {
        if (hasFrameworkStarted(frameworkApplicationInfo)) {
          updateFrameworkStage(instance, started)
          updateInstanceStatus(instance, started)
          var fwRest = getRestAddress(getLeaderTask(getApplicationInfo(frameworkName)))
          while (fwRest == null) fwRest = getRestAddress(getLeaderTask(getApplicationInfo(frameworkName)))
          updateInstanceRestAddress(instance, fwRest)
          isStarted = true
        } else {
          updateFrameworkStage(instance, starting)
          Thread.sleep(delay)
        }
      } else {
        updateFrameworkStage(instance, failed)
        throw new Exception(s"Marathon returns status code: ${getStatusCode(frameworkApplicationInfo)} " +
          s"during the start process of framework. Framework '$frameworkName' is marked as failed.")
      }
    }
  }

  private def hasFrameworkStarted(response: CloseableHttpResponse) = {
    val tasksRunning = getNumberOfRunningTasks(response)

    tasksRunning == 1
  }
}