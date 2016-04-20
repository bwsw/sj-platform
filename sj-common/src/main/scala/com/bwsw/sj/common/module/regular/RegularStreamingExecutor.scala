package com.bwsw.sj.common.module.regular

import com.bwsw.sj.common.module.entities.Transaction
import com.bwsw.sj.common.module.environment.ModuleEnvironmentManager

/**
 * Class that contains an execution logic of regular module
 * Created: 11/04/2016
 * @author Kseniya Mikhaleva
 */

abstract class RegularStreamingExecutor(manager: ModuleEnvironmentManager) {
  /**
   * Will invoke only once at the beginning of launch of module
   */
  def init(): Unit

  /**
   *
   * @param transaction
   */
  def run(transaction: Transaction): Unit

  def finish(): Unit

  def onCheckpoint(): Unit

  def onTimer(): Unit

}
