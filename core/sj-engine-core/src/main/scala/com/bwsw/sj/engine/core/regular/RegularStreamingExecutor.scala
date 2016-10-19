package com.bwsw.sj.engine.core.regular

import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager

/**
 * Class is responsible for regular module execution logic.
 * Module uses a specific instance, to personalize its work.
 * Executor provides following methods, which don't do anything by default so you should define their implementation by yourself
 *
 * @author Kseniya Mikhaleva
 */

class RegularStreamingExecutor(manager: ModuleEnvironmentManager) extends StreamingExecutor {
  /**
   * Is invoked only once at the beginning of launching of module
   */
  def onInit(): Unit = {}

  /**
   * Used for processing one envelope. It is invoked for every received message
   * from one of the inputs that are defined within the instance.
   * @param envelope A message that can have different type depending on a type of input
   *                 so you should cast the message to get certain fields.
   */
  def onMessage(envelope: Envelope): Unit = {}

  /**
   * Handler triggered before every checkpoint
   */
  def onBeforeCheckpoint(): Unit = {}

  /**
   * Handler triggered after every checkpoint
   */
  def onAfterCheckpoint(): Unit = {}

  /**
   * Is invoked every time when a set timer goes out
   *
   * @param jitter Delay between a real response time and an invocation of this handler
   */
  def onTimer(jitter: Long): Unit = {}

  /**
   * Handler triggered if idle timeout goes out but a new message hasn't appeared.
   * Nothing to execute
   */
  def onIdle(): Unit = {}

  /**
   * Handler triggered before saving of the state
   *
   * @param isFullState Flag denotes that the full state (true) or partial changes of state (false) will be saved
   */
  def onBeforeStateSave(isFullState: Boolean): Unit = {}

  /**
   * Handler triggered after saving of the state
   *
   * @param isFullState Flag denotes that there was(were) a saving of the full state (true) or partial changes of state(false)
   */
  def onAfterStateSave(isFullState: Boolean): Unit = {}

}
