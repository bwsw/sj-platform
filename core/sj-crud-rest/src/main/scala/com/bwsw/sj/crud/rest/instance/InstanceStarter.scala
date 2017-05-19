package com.bwsw.sj.crud.rest.instance

import java.net.URI

import com.bwsw.common.LeaderLatch
import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.bwsw.sj.common.dal.ConnectionConstants
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.utils.FrameworkLiterals._
import com.bwsw.sj.common.utils._
import com.bwsw.sj.crud.rest.RestLiterals
import com.bwsw.sj.crud.rest.marathon.{MarathonApplicationById, MarathonRequest}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

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

  private var leaderLatch: Option[LeaderLatch] = None

  def run() = {
    Try {
      logger.info(s"Instance: '${instance.name}'. Launch an instance.")
      updateInstanceStatus(instance, starting)
      startInstance()
      close()
    } match {
      case Success(_) =>
        logger.info(s"Instance: '${instance.name}' has been launched.")
      case Failure(e) =>
        logger.error(s"Instance: '${instance.name}'. Instance is failed during the start process.", e)
        updateInstanceStatus(instance, failed)
        leaderLatch.foreach(_.close())
        close()
    }
  }

  private def startInstance() = {
    val marathonInfo = getMarathonInfo()
    if (isStatusOK(marathonInfo)) {
      val marathonMaster = getMarathonMaster(marathonInfo)
      leaderLatch = Option(createLeaderLatch(marathonMaster))
      updateFrameworkStage(instance, starting)
      startFramework(marathonMaster)
      leaderLatch.foreach(_.close())
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
    val zkHost = Option(System.getenv("ZOOKEEPER_HOST"))
    val zkPort = Option(System.getenv("ZOOKEEPER_PORT"))
    if (zkHost.isDefined && zkPort.isDefined) {
      zooKeeperServers = zkHost.get + ":" + zkPort.get
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
    environmentVariables = environmentVariables ++ instance.environmentVariables

    environmentVariables
  }

  private def getBackoffSettings(): (Int, Double, Int) = {
    val backoffSeconds = Try(ConfigurationSettingsUtils.getFrameworkBackoffSeconds()) match {
      case Success(x) => x
      case Failure(_: NoSuchFieldException) => 7
      case Failure(e) => throw e
    }
    val backoffFactor = Try(ConfigurationSettingsUtils.getFrameworkBackoffFactor()) match {
      case Success(x) => x
      case Failure(_: NoSuchFieldException) => 7.0
      case Failure(e) => throw e
    }
    val maxLaunchDelaySeconds = Try(ConfigurationSettingsUtils.getFrameworkMaxLaunchDelaySeconds()) match {
      case Success(x) => x
      case Failure(_: NoSuchFieldException) => 600
      case Failure(e) => throw e
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
          while (fwRest.isEmpty) fwRest = getRestAddress(getLeaderTask(getApplicationInfo(frameworkName)))
          updateInstanceRestAddress(instance, fwRest.get)
          isStarted = true
        } else {
          Option(applicationParsedEntity.app.lastTaskFailure) match {
            case Some(x) =>
              destroyMarathonApplication(frameworkName)
              updateFrameworkStage(instance, failed)
              throw new Exception(s"Framework has not started due to: ${x.message}; " +
                s"Framework '$frameworkName' is marked as failed.")
            case _ =>
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