package com.bwsw.sj.common.module.environment

import com.bwsw.sj.common.module.SjTimer
import com.bwsw.sj.common.module.state.{StateStorage, IStateService}
import scala.collection.mutable

/**
 * Class allowing to manage environment of module that has state
 * Created: 15/04/2016
 * @author Kseniya Mikhaleva
 */

class StatefulModuleEnvironmentManager(stateStorage: IStateService,
                                       options: Map[String, Any],
                                       outputs: List[String],
                                       temporaryOutput: mutable.Map[String, mutable.MutableList[Array[Byte]]],
                                       moduleTimer: SjTimer) extends ModuleEnvironmentManager(options, outputs, temporaryOutput, moduleTimer) {
  /**
   * Returns specific state of module
   * @return Module state
   */
  override def getState: StateStorage = stateStorage.getState
}
