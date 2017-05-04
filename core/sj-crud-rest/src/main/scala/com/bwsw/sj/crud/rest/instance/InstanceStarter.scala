package com.bwsw.sj.crud.rest.instance

import java.net.URI

import com.bwsw.common.LeaderLatch
import com.bwsw.sj.common.DAL.ConnectionConstants
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.bwsw.sj.common.utils.FrameworkLiterals._
import com.bwsw.sj.common.utils._
import com.bwsw.sj.crud.rest.RestLiterals
import com.bwsw.sj.crud.rest.marathon.{MarathonApplicationById, MarathonRequest}
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

  private var leaderLatch: LeaderLatch = _

  def run() = {
    try {
      logger.info(s"Instance: '${instance.name}'. Launch an instance.")
      updateInstanceStatus(instance, starting)
      startInstance()
      close()
      logger.info(s"Instance: '${instance.name}' has been launched.")
    } catch {
      case e: Exception =>
        logger.error(s"Instance: '${instance.name}'. Instance is failed during the start process.", e)
        updateInstanceStatus(instance, failed)
        if (leaderLatch != null) leaderLatch.close()
        close()
    }
  }

  private def startInstance() = {
    val marathonInfo = getMarathonInfo()
    if (isStatusOK(marathonInfo)) {
      val marathonMaster = getMarathonMaster(marathonInfo)
      leaderLatch = createLeaderLatch(marathonMaster)
      updateFrameworkStage(instance, starting)
      startFramework(marathonMaster)
      leaderLatch.close()
    } else {
      updateInstanceStatus(instance, failed)
    }
  }

  private def createLeaderLatch(marathonMaster: String) = {
    logger.debug(s"Instance: '${instance.name}'. Creating a leader latch to start the instance.")
    val zkServers = getZooKeeperServers(marathonMaster)
    val leader = new LeaderLatch(Set(zkServers), RestLiterals.masterNode)
    leader.start()
    leader.takeLeadership(delay)

    leader
  }

  private def getZooKeeperServers(marathonMaster: String) = {
    logger.debug(s"Instance: '${instance.name}'. Getting a zookeeper address.")
    var zooKeeperServers = ""
    val zkHost = System.getenv("ZOOKEEPER_HOST")
    val zkPort = System.getenv("ZOOKEEPER_PORT")
    if (zkHost != null && zkPort != null) {
      zooKeeperServers = zkHost + ":" + zkPort
      logger.debug(s"Instance: '${instance.name}'. Get a zookeeper address: '$zooKeeperServers' from environment variables.")
    } else {
      val marathonMasterUrl = new URI(marathonMaster)
      zooKeeperServers = marathonMasterUrl.getHost + ":" + marathonMasterUrl.getPort
      logger.debug(s"Instance: '${instance.name}'. Get a zookeeper address: '$zooKeeperServers' from marathon.")
    }

    zooKeeperServers
  }
  
  private def startFramework(marathonMaster: String) = {
    logger.debug(s"Instance: '${instance.name}'. Try to launch or create a framework: '$frameworkName'.")
    val frameworkApplicationInfo = getApplicationInfo(frameworkName)
    if (isStatusOK(frameworkApplicationInfo)) {
      launchFramework()
    } else {
      createFramework(marathonMaster)
    }
  }

  private def launchFramework() = {
    logger.debug(s"Instance: '${instance.name}'. Launch a framework: '$frameworkName'.")
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
    logger.debug(s"Instance: '${instance.name}'. Create a framework: '$frameworkName'.")
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
      mesosMasterLabel -> marathonMaster,
      frameworkIdLabel -> frameworkName
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
      logger.debug(s"Instance: '${instance.name}'. Waiting until a framework: '$frameworkName' is launched.")
      val frameworkApplicationInfo = getApplicationInfo(frameworkName)
      if (isStatusOK(frameworkApplicationInfo)) {
        val applicationParsedEntity = getApplicationEntity(frameworkApplicationInfo)

        if (hasFrameworkStarted(applicationParsedEntity)) {
          updateFrameworkStage(instance, started)
          updateInstanceStatus(instance, started)
          var fwRest = getRestAddress(getLeaderTask(getApplicationInfo(frameworkName)))
          while (fwRest == null) fwRest = getRestAddress(getLeaderTask(getApplicationInfo(frameworkName)))
          updateInstanceRestAddress(instance, fwRest)
          isStarted = true
        } else {
          if (applicationParsedEntity.app.lastTaskFailure != null) {
            destroyMarathonApplication(frameworkName)
            updateFrameworkStage(instance, failed)
            throw new Exception(s"Framework has not started due to: ${applicationParsedEntity.app.lastTaskFailure.message}; " +
              s"Framework '$frameworkName' is marked as failed.")
          }
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

  private def hasFrameworkStarted(applicationEntity: MarathonApplicationById) = applicationEntity.app.tasksRunning == 1
}