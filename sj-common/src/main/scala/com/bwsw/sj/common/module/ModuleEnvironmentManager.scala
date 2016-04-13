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
                               temporaryOutput: mutable.Map[String, mutable.MutableList[Array[Byte]]]) {

  def getState() = stateStorage.getState()

  def setState(variables: mutable.Map[String, Any]) = stateStorage.setState(variables)

  def getOutput(streamName: String) = temporaryOutput(streamName)
}