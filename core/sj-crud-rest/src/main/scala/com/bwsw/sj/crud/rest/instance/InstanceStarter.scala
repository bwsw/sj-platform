package com.bwsw.sj.crud.rest.instance

import java.net.{InetSocketAddress, URI}
import java.util

import com.bwsw.sj.common.DAL.ConnectionConstants
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.model.{TStreamSjStream, ZKService}
import com.bwsw.sj.common.StreamConstants
import com.bwsw.sj.common.utils.ConfigUtils
import com.bwsw.sj.crud.rest.entities.MarathonRequest
import com.bwsw.sj.crud.rest.utils.StreamUtil
import com.twitter.common.quantity.{Amount, Time}
import com.twitter.common.zookeeper.DistributedLock.LockingException
import com.twitter.common.zookeeper.{DistributedLockImpl, ZooKeeperClient}
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._

/**
 * One-thread starting object for instance
 * using synchronous apache http client
 *
 *
 * @author Kseniya Tomskikh
 */
class InstanceStarter(instance: Instance, delay: Long) extends Runnable with InstanceMarathonManager {
  import com.bwsw.sj.common.ModuleConstants._
  import scala.collection.JavaConverters._

  private val logger = LoggerFactory.getLogger(getClass.getName)

  def run() = {
    logger.debug(s"Instance: ${instance.name}. Start instance.")
    try {
      val zkSessionTimeout = ConfigUtils.getZkSessionTimeout()
      val mesosInfoResponse = getMarathonInfo
      if (mesosInfoResponse.getStatusLine.getStatusCode.equals(OK)) {
        val entity = marathonEntitySerializer.deserialize[Map[String, Any]](EntityUtils.toString(mesosInfoResponse.getEntity, "UTF-8"))
        val mesosMaster = entity.get("marathon_config").get.asInstanceOf[Map[String, Any]].get("master").get.asInstanceOf[String]

        val zooKeeperServers = getZooKeeperServers(mesosMaster)
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

        var streams: Array[String] = instance.outputs
        if (!instance.moduleType.equals(inputStreamingType)) {
          streams = streams.union(instance.inputs.map(_.replaceAll("/split|/full", "")))
        }
        val streamsToStart = streams.flatMap(name => streamDAO.get(name))
          .filter(stream => stream.streamType.equals(StreamConstants.tStreamType))
          .filter(stream => !stream.asInstanceOf[TStreamSjStream].generator.generatorType.equals("local"))
        startGenerators(streamsToStart.map(stream => stream.asInstanceOf[TStreamSjStream]).toSet)

        val stages = Map(instance.stages.asScala.toList: _*)
        if (!stages.exists(s => !s._1.equals(instance.name) && s._2.state.equals(failed))) {
          instanceStart(mesosMaster)
          logger.debug(s"Instance: ${instance.name}. Instance is started.")
        } else {
          logger.debug(s"Instance: ${instance.name}. Failed instance.")
          instance.status = failed
        }
        distributedLock.unlock()
      } else {
        logger.debug(s"Instance: ${instance.name}. Failed instance.")
        instance.status = failed
      }
    } catch {
      case e: Exception =>
        logger.debug(s"Instance: ${instance.name}. Failed instance.")
        logger.debug(e.getMessage)
        e.printStackTrace()
        instance.status = failed
    }
    instanceDAO.save(instance)
  }

  private def getZooKeeperServers(mesosMaster: String) = {
    val zooKeeperServers = new util.ArrayList[InetSocketAddress]()
    val restHost = System.getenv("ZOOKEEPER_HOST")
    val restPort = System.getenv("ZOOKEEPER_PORT")
    if (restHost != null && restPort != null) {
      zooKeeperServers.add(new InetSocketAddress(restHost, restPort.toInt))
    } else {
      val mesosMasterUrl = new URI(mesosMaster)
      zooKeeperServers.add(new InetSocketAddress(mesosMasterUrl.getHost, mesosMasterUrl.getPort))
    }

    zooKeeperServers
  }

  /**
   * Starting of instance if all generators is started
   */
  def instanceStart(mesosMaster: String) = {
    logger.debug(s"Instance: ${instance.name}. Instance is starting.")
    updateInstanceStage(instance, instance.name, starting)
    val startFrameworkResult = frameworkStart(mesosMaster)
    var isStarted = false
    startFrameworkResult match {
      case Right(response) =>
        if (response.getStatusLine.getStatusCode.equals(OK) ||
          response.getStatusLine.getStatusCode.equals(Created)) {

          while (!isStarted) {
            Thread.sleep(delay)
            val taskInfoResponse = getApplicationInfo(instance.name)
            if (taskInfoResponse.getStatusLine.getStatusCode == OK) {
              val entity = marathonEntitySerializer.deserialize[Map[String, Any]](EntityUtils.toString(taskInfoResponse.getEntity, "UTF-8"))
              val tasksRunning = entity("app").asInstanceOf[Map[String, Any]]("tasksRunning").asInstanceOf[Int]
              if (tasksRunning == 1) {
                instance.status = started
                updateInstanceStage(instance, instance.name, started)
                isStarted = true
              }
            } else {
              updateInstanceStage(instance, instance.name, starting)
            }
          }
        } else {
          instance.status = failed
          updateInstanceStage(instance, instance.name, failed)
        }
      case Left(isRunning) =>
        if (!isRunning) {
          instance.status = failed
          updateInstanceStage(instance, instance.name, failed)
        } else {
          instance.status = started
          updateInstanceStage(instance, instance.name, started)
        }
    }
  }

  /**
   * Running framework on mesos
   *
   * @return - Response from marathon or flag of running framework
   *         (true, if framework running on mesos)
   */
  def frameworkStart(mesosMaster: String) = {
    logger.debug(s"Instance: ${instance.name}. Start framework for instance.")
    val frameworkJarName = ConfigUtils.getFrameworkJarName()
    val restUrl = new URI(s"$restAddress/v1/custom/jars/$frameworkJarName")
    val taskInfoResponse = getApplicationInfo(instance.name)
    if (taskInfoResponse.getStatusLine.getStatusCode.equals(OK)) {
      val ignore = marathonEntitySerializer.getIgnoreUnknown()
      marathonEntitySerializer.setIgnoreUnknown(true)
      val entity = marathonEntitySerializer.deserialize[MarathonRequest](EntityUtils.toString(taskInfoResponse.getEntity, "UTF-8"))
      marathonEntitySerializer.setIgnoreUnknown(ignore)
      if (entity.instances < 1) {
        Right(scaleMarathonApplication(instance.name, 1))
      } else {
        Left(true)
      }
    } else {
      var applicationEnvs = Map(
        "MONGO_HOST" -> ConnectionConstants.mongoHost,
        "MONGO_PORT" -> s"${ConnectionConstants.mongoPort}",
        "INSTANCE_ID" -> instance.name,
        "MESOS_MASTER" -> mesosMaster
      )
      if (instance.environmentVariables != null) {
        applicationEnvs = applicationEnvs ++ Map(instance.environmentVariables.asScala.toList: _*)
      }
      val request = new MarathonRequest(instance.name,
        "java -jar " + frameworkJarName + " $PORT",
        1,
        Map(applicationEnvs.toList: _*),
        List(restUrl.toString))

      Right(startMarathonApplication(request))
    }
  }

  /**
   * Starting generators for streams of instance
   *
   * @param streams - Streams
   * @return - Future of started generators
   */
  def startGenerators(streams: Set[TStreamSjStream]) = {
    streams.foreach { stream =>
      val generatorName = stream.name
      logger.debug(s"Instance: ${instance.name}. Try starting generator $generatorName.")
      val stage = instance.stages.get(generatorName)
      if (stage.state.equals(toHandle) || stage.state.equals(failed)) {
        var isStarted = false
        updateInstanceStage(instance, stream.name, starting)
        val startGeneratorResult = startGenerator(stream)
        startGeneratorResult match {
          case Right(response) =>
            if (response.getStatusLine.getStatusCode.equals(OK) ||
              response.getStatusLine.getStatusCode.equals(Created)) {
              while (!isStarted) {
                Thread.sleep(delay)
                val taskInfoResponse = getApplicationInfo(StreamUtil.createGeneratorTaskName(stream))
                if (taskInfoResponse.getStatusLine.getStatusCode.equals(OK)) {
                  val entity = marathonEntitySerializer.deserialize[Map[String, Any]](EntityUtils.toString(taskInfoResponse.getEntity, "UTF-8"))
                  val tasksRunning = entity("app").asInstanceOf[Map[String, Any]]("tasksRunning").asInstanceOf[Int]
                  if (tasksRunning == stream.generator.instanceCount) {
                    updateInstanceStage(instance, stream.name, started)
                    isStarted = true
                  } else {
                    updateInstanceStage(instance, stream.name, starting)
                  }
                }
              }
            } else {
              updateInstanceStage(instance, stream.name, failed)
            }

          case Left(msg) => updateInstanceStage(instance, stream.name, failed)
        }
      }
      updateInstanceStages(instance)
    }
  }

  /**
   * Starting transaction generator for stream on mesos
   *
   * @param stream - Stream for running generator
   * @return - Future with response from request to marathon
   */
  private def startGenerator(stream: TStreamSjStream) = {
    logger.debug(s"Instance: ${instance.name}. Start generator for stream ${stream.name}.")
    val transactionGeneratorJar = ConfigUtils.getTransactionGeneratorJarName()
    val zkService = stream.generator.service.asInstanceOf[ZKService]
    val generatorProvider = zkService.provider
    var prefix = zkService.namespace
    val taskId = StreamUtil.createGeneratorTaskName(stream)
    if (stream.generator.generatorType.equals("per-stream")) {
      prefix += s"/${stream.name}"
    } else {
      prefix += "/global"
    }

    val restUrl = new URI(s"$restAddress/v1/custom/jars/$transactionGeneratorJar")

    val marathonRequest = MarathonRequest(taskId,
      "java -jar " + transactionGeneratorJar + " $PORT",
      stream.generator.instanceCount,
      Map("ZK_SERVERS" -> generatorProvider.hosts.mkString(";"), "PREFIX" -> prefix),
      List(restUrl.toString))

    val taskInfoResponse = getApplicationInfo(marathonRequest.id)
    if (taskInfoResponse.getStatusLine.getStatusCode.equals(OK)) {
      val ignore = marathonEntitySerializer.getIgnoreUnknown()
      marathonEntitySerializer.setIgnoreUnknown(true)
      val entity = marathonEntitySerializer.deserialize[MarathonRequest](EntityUtils.toString(taskInfoResponse.getEntity, "UTF-8"))
      marathonEntitySerializer.setIgnoreUnknown(ignore)
      if (entity.instances < marathonRequest.instances) {
        logger.debug(s"Instance: ${instance.name}. Scaling generator ${marathonRequest.id}.")
        Right(scaleMarathonApplication(marathonRequest.id, marathonRequest.instances))
      } else {
        logger.debug(s"Instance: ${instance.name}. Generator ${marathonRequest.id} already started.")
        Left(s"Generator $taskId is already created")
      }
    } else {
      logger.debug(s"Instance: ${instance.name}. Generator ${marathonRequest.id} is starting.")
      Right(startMarathonApplication(marathonRequest))
    }
  }


  /**
   * Updating duration for all stages of instance
   *
   * @param instance - Instance for updating
   */
  private def updateInstanceStages(instance: Instance) = {
    logger.debug(s"Update stages of instance ${instance.name}")
    instance.stages.keySet().foreach { key =>
      val stage = instance.stages.get(key)
      updateInstanceStage(instance, key, stage.state)
    }
  }
}
