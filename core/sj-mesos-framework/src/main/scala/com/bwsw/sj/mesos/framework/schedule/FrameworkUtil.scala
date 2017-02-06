package com.bwsw.sj.mesos.framework.schedule

import java.io.{PrintWriter, StringWriter}
import java.net.URI

import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.mesos.framework.task.TasksList
import org.apache.log4j.Logger
import org.apache.mesos.Protos.{MasterInfo, TaskID}
import org.apache.mesos.SchedulerDriver

import scala.collection.immutable
import scala.util.Properties

/**
 *
 *
 * @author Kseniya Tomskikh
 */
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
   * @param instance current launched task
   * @return ports count for current task
   */
  def getCountPorts(instance: Instance) = {
    instance match {
      case _: OutputInstance => 2
      case regularInstance: RegularInstance => regularInstance.inputs.length + regularInstance.outputs.length + 4
      case _: InputInstance => instance.outputs.length + 2
      case windowedInstance: WindowedInstance => 1 + windowedInstance.relatedStreams.length + windowedInstance.outputs.length + 4
    }
  }

  /**
   * Handler for Scheduler Exception
   */
  def handleSchedulerException(e: Exception, logger: Logger) = {
    val sw = new StringWriter
    e.printStackTrace(new PrintWriter(sw))
    TasksList.setMessage(e.getMessage)
    logger.error(s"Framework error: ${sw.toString}")
    driver.stop()
    System.exit(1)
  }

  def getEnvParams = {
    Map(
      "instanceId" -> Properties.envOrElse("INSTANCE_ID", "00000000-0000-0000-0000-000000000000"),
      "mongodbHosts" -> Properties.envOrElse("MONGO_HOSTS", "127.0.0.1:27017")
    )
  }

  /**
   * Get jar URI for framework
   * @param instance:Instance
   * @return String
   */
  def getModuleUrl(instance: Instance): String = {
    jarName = configFileService.get("system." + instance.engine).get.value
    val restHost = configFileService.get(ConfigLiterals.hostOfCrudRestTag).get.value
    val restPort = configFileService.get(ConfigLiterals.portOfCrudRestTag).get.value.toInt
    val restAddress = new URI(s"http://$restHost:$restPort/v1/custom/jars/$jarName").toString
    logger.debug(s"Engine downloading URL: $restAddress")
    restAddress
  }

  def getInstanceStatus: String = {
    val optionInstance = ConnectionRepository.getInstanceService.get(FrameworkUtil.params("instanceId"))
    if (optionInstance.isDefined) optionInstance.get.status
    // TODO return if instance not defined
    else ""
  }

  def isInstanceStarted: Boolean = {
    updateInstance()
    instance.status == "started"
  }

  def killAllLaunchedTasks() = {
    TasksList.getLaunchedTasks.foreach(taskId => {
      TasksList.stopTask(taskId)
    })
  }

  /**
    * Teardown framework, do it if instance not started.
    */
  def teardown() = {
    println("Launched tasks: ", TasksList.getLaunchedTasks)
    killAllLaunchedTasks()
  }

  def prepareTasksToLaunch() = {
    print("Lanched tasks")
    TasksList.getList.foreach(task => {
      print(task.toJson, "\n") // TODO remove print
      if (!TasksList.getLaunchedTasks.contains(task.id)) TasksList.addToLaunch(task.id)
    })
  }


  def updateInstance() = {
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
