package com.bwsw.sj.crud.rest.instance

import java.net.URI

import com.bwsw.common.LeaderLatch
import com.bwsw.sj.common.DAL.ConnectionConstants
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.model.{TStreamSjStream, ZKService}
import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.bwsw.sj.common.rest.entities.MarathonRequest
import com.bwsw.sj.common.utils._
import com.bwsw.sj.crud.rest.RestLiterals
import org.apache.http.client.methods.CloseableHttpResponse
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

/**
 * One-thread starting object for instance
 * using synchronous apache http client
 *
 *
 * @author Kseniya Tomskikh
 */
class InstanceStarter(instance: Instance, delay: Long = 1000) extends Runnable with InstanceManager {

  import EngineLiterals._

  private val logger = LoggerFactory.getLogger(getClass.getName)
  private lazy val restHost = ConfigurationSettingsUtils.getCrudRestHost()
  private lazy val restPort = ConfigurationSettingsUtils.getCrudRestPort()
  private lazy val restAddress = new URI(s"http://$restHost:$restPort").toString

  def run() = {
    logger.debug(s"Instance: ${instance.name}. Start instance.")
    try {
      updateInstanceStatus(instance, starting)
      startInstance()
    } catch {
      case e: Exception =>
        logger.debug(s"Instance: ${instance.name}. Failed instance.")
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
      startGenerators()
      tryToStartFramework(marathonMaster)
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

  private def startGenerators() = {
    val generators = getGeneratorsToStart()
    generators.foreach(startGenerator)
  }

  private def getGeneratorsToStart() = {
    val streamsHavingGenerator = getStreamsHavingGenerator(instance)
    val generatorsToStart = streamsHavingGenerator.filter(isGeneratorAvailableForStarting)

    generatorsToStart
  }

  private def isGeneratorAvailableForStarting(stream: TStreamSjStream) = {
    hasGeneratorToHandle(stream.name) || hasGeneratorFailed(instance, stream.name)
  }

  private def hasGeneratorToHandle(name: String) = {
    val stage = instance.stages.get(name)
    stage.state.equals(toHandle)
  }

  private def startGenerator(stream: TStreamSjStream) = {
    updateGeneratorState(instance, stream.name, starting)
    val applicationID = getGeneratorApplicationID(stream)
    val generatorApplicationInfo = getApplicationInfo(applicationID)
    if (isStatusOK(generatorApplicationInfo)) {
      launchGenerator(stream, applicationID)
    } else {
      createGenerator(stream, applicationID)
    }
  }

  private def launchGenerator(stream: TStreamSjStream, applicationID: String) = {
    val instanceCount = stream.generator.instanceCount
    val response = scaleMarathonApplication(applicationID, instanceCount)
    if (isStatusOK(response)) {
      waitForGeneratorToStart(applicationID, stream.name, instanceCount)
    } else {
      updateGeneratorState(instance, stream.name, failed)
    }
  }

  private def hasGeneratorStarted(response: CloseableHttpResponse, instanceCount: Int) = {
    val tasksRunning = getNumberOfRunningTasks(response)

    tasksRunning == instanceCount
  }

  private def createGenerator(stream: TStreamSjStream, applicationID: String) = {
    val request = createRequestForGeneratorCreation(stream, applicationID)
    val response = startMarathonApplication(request)
    if (isStatusCreated(response)) {
      waitForGeneratorToStart(applicationID, stream.name, stream.generator.instanceCount)
    } else {
      updateGeneratorState(instance, stream.name, failed)
    }
  }

  private def createRequestForGeneratorCreation(stream: TStreamSjStream, applicationID: String) = {
    val transactionGeneratorJar = ConfigurationSettingsUtils.getTransactionGeneratorJarName()
    val command = "java -jar " + transactionGeneratorJar + " $PORT"
    val restUrl = new URI(s"$restAddress/v1/custom/jars/$transactionGeneratorJar")
    val environmentVariables = getGeneratorEnvironmentVariables(stream)
    val marathonRequest = MarathonRequest(
      applicationID,
      command,
      stream.generator.instanceCount,
      environmentVariables,
      List(restUrl.toString))

    marathonRequest
  }

  private def getGeneratorEnvironmentVariables(stream: TStreamSjStream) = {
    val zkService = stream.generator.service.asInstanceOf[ZKService]
    val generatorProvider = zkService.provider
    val prefix = createZookeeperPrefix(zkService.namespace, stream.generator.generatorType, stream.name)

    Map("ZK_SERVERS" -> generatorProvider.hosts.mkString(";"), "PREFIX" -> prefix)
  }

  private def createZookeeperPrefix(namespace: String, generatorType: String, name: String) = {
    var prefix = namespace
    if (generatorType == GeneratorLiterals.perStreamType) {
      prefix += s"/$name"
    } else {
      prefix += GeneratorLiterals.globalDirectory
    }

    prefix
  }

  private def waitForGeneratorToStart(applicationID: String, name: String, instanceCount: Int) = {
    var isStarted = false
    while (!isStarted) {
      val generatorApplicationInfo = getApplicationInfo(applicationID)
      if (isStatusOK(generatorApplicationInfo)) {
        if (hasGeneratorStarted(generatorApplicationInfo, instanceCount)) {
          updateGeneratorState(instance, name, started)
          isStarted = true
        } else {
          updateGeneratorState(instance, name, starting)
          Thread.sleep(delay)
        }
      } else {
        //todo error?
      }
    }
  }

  private def tryToStartFramework(marathonMaster: String) = {
    updateFrameworkState(instance, starting)
    if (haveGeneratorsStarted()) {
      startFramework(marathonMaster)
    } else {
      updateFrameworkState(instance, failed)
      updateInstanceStatus(instance, failed)
    }
  }

  private def haveGeneratorsStarted() = {
    val stages = mapAsScalaMap(instance.stages)

    !stages.exists(_._2.state == failed)
  }

  private def startFramework(marathonMaster: String) = {
    val frameworkApplicationInfo = getApplicationInfo(instance.name)
    if (isStatusOK(frameworkApplicationInfo)) {
      launchFramework()
    } else {
      createFramework(marathonMaster)
    }
  }

  private def launchFramework() = {
    val startFrameworkResult = scaleMarathonApplication(instance.name, 1)
    if (isStatusOK(startFrameworkResult)) {
      waitForFrameworkToStart()
    } else {
      updateFrameworkState(instance, failed)
      updateInstanceStatus(instance, failed)
    }
  }

  private def createFramework(marathonMaster: String) = {
    val request = createRequestForFrameworkCreation(marathonMaster)
    val startFrameworkResult = startMarathonApplication(request)
    if (isStatusCreated(startFrameworkResult)) {
      waitForFrameworkToStart()
    } else {
      updateFrameworkState(instance, failed)
      updateInstanceStatus(instance, failed)
    }
  }

  private def createRequestForFrameworkCreation(marathonMaster: String) = {
    val frameworkJarName = ConfigurationSettingsUtils.getFrameworkJarName()
    val command = "java -jar " + frameworkJarName + " $PORT"
    val restUrl = new URI(s"$restAddress/v1/custom/jars/$frameworkJarName")
    val environmentVariables = getFrameworkEnvironmentVariables(marathonMaster)
    val backoffSettings = getBackoffSettings()
    val request = MarathonRequest(
      instance.name,
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
      "MONGO_HOSTS" -> s"${ConnectionConstants.mongoHosts}",
      "INSTANCE_ID" -> instance.name,
      "MESOS_MASTER" -> marathonMaster
    )
    environmentVariables = environmentVariables ++ mapAsScalaMap(instance.environmentVariables)

    environmentVariables
  }

  private def getBackoffSettings(): (Int, Double, Int) = {
    val backoffSeconds = try ConfigurationSettingsUtils.getFrameworkBackoffSeconds() catch {case e:NoSuchFieldException => 7}
    val backoffFactor = try ConfigurationSettingsUtils.getFrameworkBackoffFactor() catch {case e:NoSuchFieldException => 7}
    val maxLaunchDelaySeconds = try ConfigurationSettingsUtils.getFrameworkMaxLaunchDelaySeconds() catch {case e:NoSuchFieldException => 600}
    (backoffSeconds, backoffFactor, maxLaunchDelaySeconds)
  }

  private def waitForFrameworkToStart() = {
    var isStarted = false
    while (!isStarted) {
      val frameworkApplicationInfo = getApplicationInfo(instance.name)
      if (isStatusOK(frameworkApplicationInfo)) {
        if (hasFrameworkStarted(frameworkApplicationInfo)) {
          updateFrameworkState(instance, started)
          updateInstanceStatus(instance, started)
          isStarted = true
        } else {
          updateFrameworkState(instance, starting)
          Thread.sleep(delay)
        }
      } else {
        //todo error?
      }
    }
  }

  private def hasFrameworkStarted(response: CloseableHttpResponse) = {
    val tasksRunning = getNumberOfRunningTasks(response)

    tasksRunning == 1
  }
}