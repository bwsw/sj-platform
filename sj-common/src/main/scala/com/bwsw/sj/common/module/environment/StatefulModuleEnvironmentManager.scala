package com.bwsw.sj.common.module.environment

import com.bwsw.sj.common.module.SjTimer
import com.bwsw.sj.common.module.state.StateStorage

import scala.collection.mutable

/**
 * Class allowing to manage environment of module that has state
 * Created: 15/04/2016
 * @author Kseniya Mikhaleva
 *
 * @param stateStorage
 * @param options User defined options from instance parameters
 * @param temporaryOutput Provides a store for each output stream from instance parameters
 * @param moduleTimer
 */

class StatefulModuleEnvironmentManager(stateStorage: StateStorage,
                                       options: Map[String, Any],
                                       temporaryOutput: mutable.Map[String, (String, Any)],
                                       moduleTimer: SjTimer) extends ModuleEnvironmentManager(options, temporaryOutput, moduleTimer) {
  /**
   * Returns specific state of module
   * @return Module state
   */
  override def getState: StateStorage = stateStorage
}
