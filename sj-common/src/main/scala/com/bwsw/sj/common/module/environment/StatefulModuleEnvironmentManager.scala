package com.bwsw.sj.common.module.environment

import com.bwsw.sj.common.module.ModuleTimer
import com.bwsw.sj.common.module.state.ModuleStateStorage
import scala.collection.mutable

/**
 * Class allowing to manage environment of module that has state
 * Created: 15/04/2016
 * @author Kseniya Mikhaleva
 */

class StatefulModuleEnvironmentManager(stateStorage: ModuleStateStorage,
                                       options: Map[String, Any],
                                       outputs: List[String],
                                       temporaryOutput: mutable.Map[String, mutable.MutableList[Array[Byte]]],
                                       moduleTimer: ModuleTimer) extends ModuleEnvironmentManager(options, outputs, temporaryOutput, moduleTimer) {

  override def getState() = stateStorage.getState()
}
