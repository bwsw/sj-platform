package com.bwsw.sj.mesos.framework.schedule

import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.ModuleConstants

/**
  * Created: 21/07/2016
  *
  * @author Kseniya Tomskikh
  */
object FrameworkUtil {

  def getCountPorts(instance: Instance) = {
    instance.moduleType match {
      case ModuleConstants.outputStreamingType => 2
      case ModuleConstants.regularStreamingType => instance.inputs.length + instance.outputs.length + 4
      case ModuleConstants.inputStreamingType => instance.outputs.length + 2
      case _ => 0
    }
  }

}
