package com.bwsw.sj.mesos.framework.schedule

import java.io.{PrintWriter, StringWriter}

import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.mesos.framework.task.TasksList
import org.apache.log4j.Logger
import org.apache.mesos.SchedulerDriver

/**
  *
  *
  * @author Kseniya Tomskikh
  */
object FrameworkUtil {

  /**
    * Count how much ports must be for current task.
    * @param instance current launched task
    * @return ports count for current task
    */
  def getCountPorts(instance: Instance) = {
    instance.moduleType match {
      case EngineLiterals.outputStreamingType => 2
      case EngineLiterals.regularStreamingType => instance.inputs.length + instance.outputs.length + 4
      case EngineLiterals.inputStreamingType => instance.outputs.length + 2
      case _ => 0
    }
  }

  /**
    * Handler for Scheduler Exception
    */
  def handleSchedulerException(e: Exception, driver: SchedulerDriver, logger: Logger) = {
    val sw = new StringWriter
    e.printStackTrace(new PrintWriter(sw))
    TasksList.message = e.getMessage
    logger.error(s"Framework error: ${sw.toString}")
    driver.stop()
    System.exit(1)
  }

}
