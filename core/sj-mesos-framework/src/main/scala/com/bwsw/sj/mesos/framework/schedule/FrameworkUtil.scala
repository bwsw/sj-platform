package com.bwsw.sj.mesos.framework.schedule

import java.io.{PrintWriter, StringWriter}
import java.net.URI

import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.model.instance._
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.utils.{CommonAppConfigNames, FrameworkLiterals}
import com.bwsw.sj.mesos.framework.task.TasksList
import com.typesafe.config.ConfigFactory
import org.apache.log4j.Logger
import org.apache.mesos.Protos.MasterInfo
import org.apache.mesos.SchedulerDriver

import scala.collection.immutable
import scala.util.Try


object FrameworkUtil {
  var master: Option[MasterInfo] = None
  var frameworkId: Option[String] = None
  var driver: Option[SchedulerDriver] = None
  var jarName: Option[String] = None
  var instance: Option[InstanceDomain] = None
  val configRepository: GenericMongoRepository[ConfigurationSettingDomain] = ConnectionRepository.getConfigRepository
  private val logger = Logger.getLogger(this.getClass)
  var params: Map[String, String] = immutable.Map[String, String]()

  /**
    * Count how much ports must be for current task.
    *
    * @param instance current launched task
    * @return ports count for current task
    */
  def getCountPorts(instance: InstanceDomain): Int = {
    instance match {
      case _: OutputInstanceDomain => 2
      case regularInstance: RegularInstanceDomain => regularInstance.inputs.length + regularInstance.outputs.length + 4
      case _: InputInstanceDomain => instance.outputs.length + 2
      case batchInstance: BatchInstanceDomain => batchInstance.inputs.length + batchInstance.outputs.length + 4
    }
  }

  /**
    * Handler for Scheduler Exception
    */
  def handleSchedulerException(e: Exception, logger: Logger): Unit = {
    val sw = new StringWriter
    e.printStackTrace(new PrintWriter(sw))
    TasksList.setMessage(e.getMessage)
    logger.error(s"Framework error: ${sw.toString}")
    driver.foreach(_.stop())
    System.exit(1)
  }

  def getEnvParams: Map[String, String] = {
    val config = ConfigFactory.load()

    Map(
      "instanceId" ->
        Try(config.getString(FrameworkLiterals.instanceId)).getOrElse("00000000-0000-0000-0000-000000000000"),
      "mongodbHosts" -> Try(config.getString(CommonAppConfigNames.mongoHosts)).getOrElse("127.0.0.1:27017")
    )
  }

  /**
    * Get jar URI for framework
    *
    * @param instance :Instance
    * @return String
    */
  def getModuleUrl(instance: InstanceDomain): String = {
    jarName = configRepository.get("system." + instance.engine).map(_.value)
    val restHost = configRepository.get(ConfigLiterals.hostOfCrudRestTag).get.value
    val restPort = configRepository.get(ConfigLiterals.portOfCrudRestTag).get.value.toInt
    val restAddress = new URI(s"http://$restHost:$restPort/v1/custom/jars/${jarName.get}").toString
    logger.debug(s"Engine downloading URL: $restAddress.")
    restAddress
  }

  def isInstanceStarted: Boolean = {
    updateInstance()
    instance.exists(_.status == "started")
  }

  def killAllLaunchedTasks(): Unit = {
    TasksList.getLaunchedTasks.foreach(taskId => {
      TasksList.killTask(taskId)
    })
  }

  /**
    * Teardown framework, do it if instance not started.
    */
  def teardown(): Unit = {
    logger.info(s"Kill all launched tasks: ${TasksList.getLaunchedTasks}")
    killAllLaunchedTasks()
  }

  /**
    * Selecting which tasks would be launched
    */
  def prepareTasksToLaunch(): Unit = {
    TasksList.getList.foreach(task => {
      if (!TasksList.getLaunchedTasks.contains(task.id)) TasksList.addToLaunch(task.id)
    })
    logger.info(s"Selecting tasks to launch: ${TasksList.toLaunch}")
  }


  def updateInstance(): Any = {
    val optionInstance = ConnectionRepository.getInstanceRepository.get(FrameworkUtil.params("instanceId"))

    if (optionInstance.isEmpty) {
      logger.error(s"Not found instance")
      TasksList.setMessage("Framework shut down: not found instance.")
      driver.foreach(_.stop())
    } else {
      FrameworkUtil.instance = optionInstance
    }
  }
}
