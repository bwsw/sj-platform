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
   *Used for processing one transaction. Will invoke for every transaction
   */
  def onTxn(txn: Transaction): Unit

  def finish(): Unit

  def onBeforeCheckpoint(): Unit
  
  def onAfterCheckpoint(): Unit

  def onTimer(jitter: Long): Unit
  
  def onIdle(): Unit

  def onMessage(): Unit

  /**
   * Handler triggered before save state
   * @param isFullState Flag denotes that full state (true) or partial changes of state (false) will be saved
   */
  def onBeforeStateSave(isFullState: Boolean): Unit

  /**
   * Handler triggered after save state
   * @param isFullState Flag denotes that there was save of full state (true) or partial changes of state(false)
   */
  def onAfterStateSave(isFullState: Boolean): Unit

}
