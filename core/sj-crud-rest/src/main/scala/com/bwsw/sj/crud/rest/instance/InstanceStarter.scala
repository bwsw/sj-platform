package com.bwsw.sj.crud.rest.instance

import java.net.URI

import com.bwsw.common.LeaderLatch
import com.bwsw.common.http.HttpClient
import com.bwsw.common.http.HttpStatusChecker._
import com.bwsw.common.marathon.{MarathonApi, MarathonApplication, MarathonRequest}
import com.bwsw.sj.common.config.SettingsUtils
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.utils.FrameworkLiterals._
import com.bwsw.sj.common.utils._
import com.bwsw.sj.crud.rest.utils.RestLiterals
import org.slf4j.LoggerFactory
import scaldi.Injectable.inject
import scaldi.Injector

import scala.util.{Failure, Success, Try}

/**
  * One-thread starting object for instance
  * using synchronous apache http client
  *
  * protected methods and variables need for testing purposes
  *
  * @author Kseniya Tomskikh
  */
class InstanceStarter(instance: Instance,
                      marathonAddress: String,
                      zookeeperHost: Option[String] = None,
                      zookeeperPort: Option[Int] = None,
                      delay: Long = 1000,
                      marathonTimeout: Int = 60000)(implicit val injector: Injector) extends Runnable {

  import EngineLiterals._

  private val logger = LoggerFactory.getLogger(getClass.getName)
  private val settingsUtils = inject[SettingsUtils]
  private lazy val restHost = settingsUtils.getCrudRestHost()
  private lazy val restPort = settingsUtils.getCrudRestPort()
  private lazy val restAddress = RestLiterals.createUri(restHost, restPort)
  protected val instanceManager = new InstanceDomainRenewer()
  protected val client = new HttpClient(marathonTimeout)
  protected val marathonManager = new MarathonApi(client, marathonAddress)
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

  protected def startInstance(): Unit = {
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

  protected def createLeaderLatch(marathonMaster: String): LeaderLatch = {
    logger.debug(s"Instance: '${instance.name}'. Creating a leader latch to start the instance.")
    val zkServers = getZooKeeperServers(marathonMaster)
    val leader = new LeaderLatch(Set(zkServers), RestLiterals.masterNode)
    leader.start()
    leader.takeLeadership(delay)

    leader
  }

  protected def getZooKeeperServers(marathonMaster: String): String = {
    logger.debug(s"Instance: '${instance.name}'. Getting a zookeeper address.")
    (zookeeperHost, zookeeperPort) match {
      case (Some(zkHost), Some(zkPort)) =>
        val zooKeeperServers = s"$zkHost:$zkPort"
        logger.debug(s"Instance: '${instance.name}'. Get a zookeeper address: '$zooKeeperServers'.")
        zooKeeperServers

      case _ =>
        val marathonMasterUrl = new URI(marathonMaster)
        val zooKeeperServers = marathonMasterUrl.getHost + ":" + marathonMasterUrl.getPort
        logger.debug(s"Instance: '${instance.name}'. Get a zookeeper address: '$zooKeeperServers' from marathon.")
        zooKeeperServers
    }
  }

  protected def startFramework(marathonMaster: String): Unit = {
    logger.debug(s"Instance: '${instance.name}'. Try to launch or create a framework: '$frameworkName'.")
    val frameworkApplicationInfo = marathonManager.getApplicationInfo(frameworkName)
    if (isStatusOK(frameworkApplicationInfo)) {
      launchFramework()
    } else {
      createFramework(marathonMaster)
    }
  }

  protected def launchFramework(): Unit = {
    logger.debug(s"Instance: '${instance.name}'. Launch a framework: '$frameworkName'.")
    val startFrameworkResult = marathonManager.scaleMarathonApplication(frameworkName, 1)
    if (isStatusOK(startFrameworkResult)) {
      waitForFrameworkToStart()
    } else {
      instanceManager.updateFrameworkStage(instance, failed)
      instanceManager.updateInstanceStatus(instance, failed)
      instanceManager.updateInstanceRestAddress(instance, None)
    }
  }

  protected def createFramework(marathonMaster: String): Unit = {
    logger.debug(s"Instance: '${instance.name}'. Create a framework: '$frameworkName'.")
    val request = createRequestForFrameworkCreation(marathonMaster)
    val startFrameworkResult = marathonManager.startMarathonApplication(request)
    if (isStatusCreated(startFrameworkResult)) {
      waitForFrameworkToStart()
    } else {
      instanceManager.updateFrameworkStage(instance, failed)
      instanceManager.updateInstanceStatus(instance, failed)
      instanceManager.updateInstanceRestAddress(instance, None)
    }
  }

  protected def createRequestForFrameworkCreation(marathonMaster: String): MarathonRequest = {
    val frameworkJarName = settingsUtils.getFrameworkJarName()
    val command = FrameworkLiterals.createCommandToLaunch(frameworkJarName)
    val restUrl = new URI(s"$restAddress/v1/custom/jars/$frameworkJarName")
    val environmentVariables = getFrameworkEnvironmentVariables(marathonMaster)
    val backoffSettings = settingsUtils.getBackoffSettings()
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

  protected def getFrameworkEnvironmentVariables(marathonMaster: String): Map[String, String] = {
    var environmentVariables = Map(
      instanceIdLabel -> instance.name,
      mesosMasterLabel -> marathonMaster,
      frameworkIdLabel -> frameworkName
    )
    environmentVariables = environmentVariables ++ inject[ConnectionRepository].mongoEnvironment
    environmentVariables = environmentVariables ++ instance.environmentVariables

    environmentVariables
  }

  protected def waitForFrameworkToStart(): Unit = {
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
          instanceManager.updateInstanceRestAddress(instance, fwRest)
          isStarted = true
        } else {
          Option(applicationParsedEntity.app.lastTaskFailure) match {
            case Some(x) =>
              marathonManager.destroyMarathonApplication(frameworkName)
              instanceManager.updateFrameworkStage(instance, failed)
              instanceManager.updateInstanceRestAddress(instance, None)
              throw new InterruptedException(s"Framework has not started due to: ${x.message}; " +
                s"Framework '$frameworkName' is marked as failed.")
            case _ =>
          }
          instanceManager.updateFrameworkStage(instance, starting)
          Thread.sleep(delay)
        }
      } else {
        instanceManager.updateFrameworkStage(instance, failed)
        instanceManager.updateInstanceRestAddress(instance, None)
        throw new Exception(s"Marathon returns status code: ${getStatusCode(frameworkApplicationInfo)} " +
          s"during the start process of framework. Framework '$frameworkName' is marked as failed.")
      }
    }
  }

  protected def hasFrameworkStarted(applicationEntity: MarathonApplication): Boolean = applicationEntity.app.tasksRunning == 1
}

class InstanceStarterBuilder(implicit val injector: Injector) {
  def apply(instance: Instance,
            marathonAddress: String,
            zookeeperHost: Option[String] = None,
            zookeeperPort: Option[Int] = None,
            delay: Long = 1000,
            marathonTimeout: Int = 60000): InstanceStarter = {
    new InstanceStarter(
      instance,
      marathonAddress,
      zookeeperHost,
      zookeeperPort,
      delay,
      marathonTimeout)
  }
}
