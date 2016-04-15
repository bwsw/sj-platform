package com.bwsw.sj.common.module


import com.bwsw.sj.common.module.state.ModuleStateStorage

import scala.collection.mutable

/**
 * Class allowing to manage environment of module
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

class ModuleEnvironmentManager(val options: Map[String, Any],
                               stateStorage: ModuleStateStorage,
                               outputs: List[String],
                               temporaryOutput: mutable.Map[String, mutable.MutableList[Array[Byte]]],
                                moduleTimer: ModuleTimer) {

  def getState() = stateStorage.getState()

  def getOutput(streamName: String) = temporaryOutput(streamName)

  def setTimer(delay: Long) = moduleTimer.setTimer(delay)
}