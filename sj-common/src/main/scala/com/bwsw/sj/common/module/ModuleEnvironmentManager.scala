package com.bwsw.sj.common.module


import com.bwsw.sj.common.module.state.DefaultModuleStateStorage

import scala.collection.mutable

/**
 * Class allowing to manage environment of module
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

class ModuleEnvironmentManager(stateStorage: String, outputs: List[String], temporaryOutput: mutable.Map[String, mutable.MutableList[Array[Byte]]]) {

  private val state = stateStorage match {
    case "RAM" => new DefaultModuleStateStorage()
  }

  def getState = state.getState()

  def setState(variables: Map[String, Any]) = state.setState(variables)

  def getOutput(streamName: String) = temporaryOutput(streamName)
}