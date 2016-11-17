package com.bwsw.sj.mesos.framework.schedule

import java.io.{PrintWriter, StringWriter}

import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.mesos.framework.task.TasksList
import org.apache.log4j.Logger
import org.apache.mesos.Protos.MasterInfo
import org.apache.mesos.{MesosSchedulerDriver, SchedulerDriver}

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

  var instance: Instance = null

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
      case windowedInstance: WindowedInstance => 1 //todo
    }
  }

  /**
    * Handler for Scheduler Exception
    */
  def handleSchedulerException(e: Exception, logger: Logger) = {
    val sw = new StringWriter
    e.printStackTrace(new PrintWriter(sw))
    TasksList.message = e.getMessage
    logger.error(s"Framework error: ${sw.toString}")
    driver.stop()
    System.exit(1)
  }


  def getEnvParams() = {
    Map(
      ("instanceId", Properties.envOrElse("INSTANCE_ID", "00000000-0000-0000-0000-000000000000")),
      ("mongodbHost", Properties.envOrElse("MONGO_HOST", "127.0.0.1")),
      ("mongodbPort", Properties.envOrElse("MONGO_PORT", "27017"))
    )
  }

}
