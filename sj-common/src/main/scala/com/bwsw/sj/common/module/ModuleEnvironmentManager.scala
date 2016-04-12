package com.bwsw.sj.common.module


import com.bwsw.sj.common.module.entities.{DefaultModuleStateStorage, LaunchParameters}

import scala.collection.mutable

/**
 * Class allows to manage environment of module
 * Created: 07/04/2016
 * @author Kseniya Mikhaleva
 */

class ModuleEnvironmentManager(launchParameters: LaunchParameters) {

  private val state = launchParameters.stateStorage match {
    case "RAM" => new DefaultModuleStateStorage()
  }

  private val temporaryOutput: mutable.Map[String, List[Array[Byte]]] = mutable.Map(launchParameters.outputs.map(x => (x, List[Array[Byte]]())): _*)

  def getState = state.getState()

  def setState(variables: Map[String, Any]) = state.setState(variables)

  def getOutput(streamName: String) = temporaryOutput(streamName)

  def setOutput(streamName: String, data: List[Array[Byte]]) = {
    temporaryOutput(streamName) = data
  }
}
