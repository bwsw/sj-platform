package com.bwsw.sj.common.module.environment

import com.bwsw.sj.common.module.SjTimer

import scala.collection.mutable

/**
 * Class allowing to manage environment of module
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

class ModuleEnvironmentManager(val options: Map[String, Any],
                               outputs: List[String],
                               temporaryOutput: mutable.Map[String, mutable.MutableList[Array[Byte]]],
                               moduleTimer: SjTimer) {

  def getOutput(streamName: String) = temporaryOutput(streamName)

  def setTimer(delay: Long) = moduleTimer.setTimer(delay)

  def getState(): mutable.Map[String, Any] = throw new Exception("Module has no state")
}




