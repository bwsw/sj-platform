package com.bwsw.sj.common.module.environment

import com.bwsw.sj.common.module.SjTimer
import com.bwsw.sj.common.module.state.StateStorage

import scala.collection.mutable

/**
 * Provides for user methods that can be used in a module
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 *
 * @param options
 * @param outputs
 * @param temporaryOutput
 * @param moduleTimer
 */

class ModuleEnvironmentManager(val options: Map[String, Any],
                               outputs: List[String],
                               temporaryOutput: mutable.Map[String, mutable.MutableList[Array[Byte]]],
                               moduleTimer: SjTimer) {

  /**
   * Allows getting specific output
   * @param streamName Name of output stream
   * @return Store of output that wrapping output stream
   */
  def getOutput(streamName: String): mutable.MutableList[Array[Byte]] = temporaryOutput(streamName)

  /**
   * Enables user to use a timer in a module which will invoke the time handler: onTimer
   * @param delay Time after which the handler will call
   */
  def setTimer(delay: Long) = moduleTimer.setTimer(delay)

  /**
   * Provides a default method for getting state of module. Must be overridden in stateful module
   * @return Module state
   */
  def getState: StateStorage = throw new Exception("Module has no state")
}




