package com.bwsw.sj.crud.rest.instance

import java.net.URI

import com.bwsw.common.LeaderLatch
import com.bwsw.common.http.HttpClient
import com.bwsw.common.marathon.{MarathonApi, MarathonApplicationById, MarathonRequest}
import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.bwsw.sj.common.dal.ConnectionConstants
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.utils.FrameworkLiterals._
import com.bwsw.sj.common.utils._
import com.bwsw.sj.crud.rest.utils.RestLiterals
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import scaldi.Injector

import scala.util.{Failure, Success, Try}
import com.bwsw.common.http.HttpStatusChecker._

/**
  * One-thread starting object for instance
  * using synchronous apache http client
  *
  * @author Kseniya Tomskikh
  */
class InstanceStarter(instance: Instance, delay: Long = 1000)(implicit val injector: Injector) extends Runnable {

  import EngineLiterals._

  private val logger = LoggerFactory.getLogger(getClass.getName)
  private lazy val restHost = ConfigurationSettingsUtils.getCrudRestHost()
  private lazy val restPort = ConfigurationSettingsUtils.getCrudRestPort()
  private lazy val restAddress = new URI(s"http://$restHost:$restPort").toString
  private val instanceManager = new InstanceDomainRenewer()
  private val marathonTimeout = ConfigurationSettingsUtils.getMarathonTimeout()
  private val client = new HttpClient(marathonTimeout)
  private val marathonManager = new MarathonApi(client)
  private val frameworkName = InstanceAdditionalFieldCreator.getFrameworkName(instance)

  private var leaderLatch: Option[LeaderLatch] = None

  def run(): Unit = {
    Try {
      logger.info(s"Instance: '${instance.name}'. Launch an instance.")
      instanceManager.updateInstanceStatus(instance, starting)
      startInstance()
      client.close()
    } match {
      case Success(_) =>
        logger.info(s"Instance: '${instance.name}' has been launched.")
      case Failure(e) =>
        logger.error(s"Instance: '${instance.name}'. Instance is failed during the start process.", e)
        instanceManager.updateInstanceStatus(instance, failed)
        leaderLatch.foreach(_.close())
        client.close()
    }
  }

  private def startInstance() = {
    val marathonInfo = marathonManager.getMarathonInfo()
    if (isStatusOK(marathonInfo)) {
      val marathonMaster = marathonManager.getMarathonMaster(marathonInfo)
      leaderLatch = Option(createLeaderLatch(marathonMaster))
      instanceManager.updateFrameworkStage(instance, starting)
      startFramework(marathonMaster)
      leaderLatch.foreach(_.close())
    } else {
      instanceManager.updateInstanceStatus(instance, failed)
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
    Try {
      val config = ConfigFactory.load()
      (config.getString(CommonAppConfigNames.zooKeeperHost),
        config.getInt(CommonAppConfigNames.zooKeeperPort))
    } match {
      case Success((zkHost, zkPort)) =>
        val zooKeeperServers = s"$zkHost:$zkPort"
        logger.debug(s"Instance: '${instance.name}'. Get a zookeeper address: '$zooKeeperServers' from application configuration.")
        zooKeeperServers

      case Failure(_) =>
        val marathonMasterUrl = new URI(marathonMaster)
        val zooKeeperServers = marathonMasterUrl.getHost + ":" + marathonMasterUrl.getPort
        logger.debug(s"Instance: '${instance.name}'. Get a zookeeper address: '$zooKeeperServers' from marathon.")
        zooKeeperServers
    }
  }

  private def startFramework(marathonMaster: String) = {
    logger.debug(s"Instance: '${instance.name}'. Try to launch or create a framework: '$frameworkName'.")
    val frameworkApplicationInfo = marathonManager.getApplicationInfo(frameworkName)
    if (isStatusOK(frameworkApplicationInfo)) {
      launchFramework()
    } else {
      createFramework(marathonMaster)
    }
  }

  private def launchFramework() = {
    logger.debug(s"Instance: '${instance.name}'. Launch a framework: '$frameworkName'.")
    val startFrameworkResult = marathonManager.scaleMarathonApplication(frameworkName, 1)
    if (isStatusOK(startFrameworkResult)) {
      waitForFrameworkToStart()
    } else {
      instanceManager.updateFrameworkStage(instance, failed)
      instanceManager.updateInstanceStatus(instance, failed)
      instanceManager.updateInstanceRestAddress(instance, "")
    }
  }

  private def createFramework(marathonMaster: String) = {
    logger.debug(s"Instance: '${instance.name}'. Create a framework: '$frameworkName'.")
    val request = createRequestForFrameworkCreation(marathonMaster)
    val startFrameworkResult = marathonManager.startMarathonApplication(request)
    if (isStatusCreated(startFrameworkResult)) {
      waitForFrameworkToStart()
    } else {
      instanceManager.updateFrameworkStage(instance, failed)
      instanceManager.updateInstanceStatus(instance, failed)
      instanceManager.updateInstanceRestAddress(instance, "")
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
      val frameworkApplicationInfo = marathonManager.getApplicationInfo(frameworkName)
      if (isStatusOK(frameworkApplicationInfo)) {
        val applicationParsedEntity = marathonManager.getApplicationEntity(frameworkApplicationInfo)

        if (hasFrameworkStarted(applicationParsedEntity)) {
          instanceManager.updateFrameworkStage(instance, started)
          instanceManager.updateInstanceStatus(instance, started)
          var fwRest = InstanceAdditionalFieldCreator.getRestAddress(marathonManager.getLeaderTask(marathonManager.getApplicationInfo(frameworkName)))
          while (fwRest.isEmpty) fwRest = InstanceAdditionalFieldCreator.getRestAddress(marathonManager.getLeaderTask(marathonManager.getApplicationInfo(frameworkName)))
          instanceManager.updateInstanceRestAddress(instance, fwRest.get)
          isStarted = true
        } else {
          Option(applicationParsedEntity.app.lastTaskFailure) match {
            case Some(x) =>
              marathonManager.destroyMarathonApplication(frameworkName)
              instanceManager.updateFrameworkStage(instance, failed)
              throw new Exception(s"Framework has not started due to: ${x.message}; " +
                s"Framework '$frameworkName' is marked as failed.")
            case _ =>
          }
          instanceManager.updateFrameworkStage(instance, starting)
          Thread.sleep(delay)
        }
      } else {
        instanceManager.updateFrameworkStage(instance, failed)
        throw new Exception(s"Marathon returns status code: ${getStatusCode(frameworkApplicationInfo)} " +
          s"during the start process of framework. Framework '$frameworkName' is marked as failed.")
      }
    }
  }

  private def hasFrameworkStarted(applicationEntity: MarathonApplicationById) = applicationEntity.app.tasksRunning == 1
}