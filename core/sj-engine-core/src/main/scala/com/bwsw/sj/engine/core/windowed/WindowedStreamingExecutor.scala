package com.bwsw.sj.engine.core.windowed

import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager

/**
 * Class is responsible for windowed module execution logic.
 * Module uses a specific instance, to personalize its work.
 * Executor provides following methods, which don't do anything by default so you should define their implementation by yourself
 *
 * @author Kseniya Mikhaleva
 */


class WindowedStreamingExecutor(manager: ModuleEnvironmentManager) extends StreamingExecutor {
  /**
   * Is invoked only once at the beginning of launching of module
   */
  def onInit(): Unit = {}

  /**
   * Used for processing one envelope. It is invoked for every received message
   * from one of the inputs that are defined within the instance.
   * @param stream A stream name in which a window is collected.
   *
   */
  def onWindow(stream: String, windowRepository: WindowRepository): Unit = {
    println("onWindow() " + windowRepository.getAll().map(x => (x._1, x._2.batches.flatMap(x => x.transactions.map(_.id)))))
  }

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

