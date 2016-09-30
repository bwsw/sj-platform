package com.bwsw.sj.crud.rest.instance

import java.net.{InetSocketAddress, URI}
import java.util

import com.bwsw.sj.common.DAL.ConnectionConstants
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.model.{TStreamSjStream, ZKService}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.entities.MarathonRequest
import com.bwsw.sj.common.utils._
import com.twitter.common.quantity.{Amount, Time}
import com.twitter.common.zookeeper.DistributedLock.LockingException
import com.twitter.common.zookeeper.{DistributedLockImpl, ZooKeeperClient}
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
  private lazy val restHost = ConfigSettingsUtils.getCrudRestHost()
  private lazy val restPort = ConfigSettingsUtils.getCrudRestPort()
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
      val distributedLock = becomeMaster(marathonMaster)
      startGenerators()
      tryToStartFramework(marathonMaster)
      distributedLock.unlock()
    } else {
      updateInstanceStatus(instance, failed)
    }
  }

  private def becomeMaster(marathonMaster: String) = {
    val zooKeeperServers = getZooKeeperServers(marathonMaster)
    val zkSessionTimeout = ConfigSettingsUtils.getZkSessionTimeout()
    val zkClient = new ZooKeeperClient(Amount.of(zkSessionTimeout, Time.MILLISECONDS), zooKeeperServers)
    var isMaster = false
    val zkLockNode = new URI(s"/rest/instance/lock").normalize()
    val distributedLock = new DistributedLockImpl(zkClient, zkLockNode.toString)
    while (!isMaster) {
      try {
        distributedLock.lock()
        isMaster = true
      } catch {
        case e: LockingException => Thread.sleep(delay)
      }
    }

    distributedLock
  }

  private def getZooKeeperServers(mesosMaster: String) = {
    val zooKeeperServers = new util.ArrayList[InetSocketAddress]()
    val restHost = System.getenv("ZOOKEEPER_HOST")
    val restPort = System.getenv("ZOOKEEPER_PORT")
    if (restHost != null && restPort != null) {
      zooKeeperServers.add(new InetSocketAddress(restHost, restPort.toInt))
    } else {
      val mesosMasterUrl = new URI(mesosMaster)
      val address = new InetSocketAddress(mesosMasterUrl.getHost, mesosMasterUrl.getPort)
      zooKeeperServers.add(address)
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
    val transactionGeneratorJar = ConfigSettingsUtils.getTransactionGeneratorJarName()
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
      prefix += "/global"
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
    val frameworkJarName = ConfigSettingsUtils.getFrameworkJarName()
    val command = "java -jar " + frameworkJarName + " $PORT"
    val restUrl = new URI(s"$restAddress/v1/custom/jars/$frameworkJarName")
    val environmentVariables = getFrameworkEnvironmentVariables(marathonMaster)
    val request = MarathonRequest(
      instance.name,
      command,
      1,
      environmentVariables,
      List(restUrl.toString))

    request
  }

  private def getFrameworkEnvironmentVariables(marathonMaster: String) = {
    var environmentVariables = Map(
      "MONGO_HOST" -> ConnectionConstants.mongoHost,
      "MONGO_PORT" -> s"${ConnectionConstants.mongoPort}",
      "INSTANCE_ID" -> instance.name,
      "MESOS_MASTER" -> marathonMaster
    )
    environmentVariables = environmentVariables ++ mapAsScalaMap(instance.environmentVariables)

    environmentVariables
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