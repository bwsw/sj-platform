package com.bwsw.sj.mesos.framework.schedule

import java.io.{PrintWriter, StringWriter}
import java.net.URI

import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.mesos.framework.task.TasksList
import org.apache.log4j.Logger
import org.apache.mesos.Protos.MasterInfo
import org.apache.mesos.SchedulerDriver

import scala.collection.immutable
import scala.util.Properties


object FrameworkUtil {
  var master: MasterInfo = null
  var frameworkId: String = null
  var driver: SchedulerDriver = null
  var jarName: String = null
  var instance: Instance = null
  val configFileService = ConnectionRepository.getConfigService
  private val logger = Logger.getLogger(this.getClass)
  var params = immutable.Map[String, String]()

  /**
   * Count how much ports must be for current task.
    *
    * @param instance current launched task
   * @return ports count for current task
   */
  def getCountPorts(instance: Instance): Int = {
    instance match {
      case _: OutputInstance => 2
      case regularInstance: RegularInstance => regularInstance.inputs.length + regularInstance.outputs.length + 4
      case _: InputInstance => instance.outputs.length + 2
      case batchInstance: BatchInstance => batchInstance.inputs.length + batchInstance.outputs.length + 4
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
    driver.stop()
    System.exit(1)
  }

  def getEnvParams: Map[String, String] = {
    Map(
      "instanceId" -> Properties.envOrElse("INSTANCE_ID", "00000000-0000-0000-0000-000000000000"),
      "mongodbHosts" -> Properties.envOrElse("MONGO_HOSTS", "127.0.0.1:27017")
    )
  }

  /**
   * Get jar URI for framework
    *
    * @param instance:Instance
   * @return String
   */
  def getModuleUrl(instance: Instance): String = {
    jarName = configFileService.get("system." + instance.engine).get.value
    val restHost = configFileService.get(ConfigLiterals.hostOfCrudRestTag).get.value
    val restPort = configFileService.get(ConfigLiterals.portOfCrudRestTag).get.value.toInt
    val restAddress = new URI(s"http://$restHost:$restPort/v1/custom/jars/$jarName").toString
    logger.debug(s"Engine downloading URL: $restAddress.")
    restAddress
  }

  def isInstanceStarted: Boolean = {
    updateInstance()
    instance.status == "started"
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
    val optionInstance = ConnectionRepository.getInstanceService.get(FrameworkUtil.params("instanceId"))

    if (optionInstance.isEmpty) {
      logger.error(s"Not found instance")
      TasksList.setMessage("Framework shut down: not found instance.")
      driver.stop()
    } else {
      FrameworkUtil.instance = optionInstance.get
    }
  }
}
