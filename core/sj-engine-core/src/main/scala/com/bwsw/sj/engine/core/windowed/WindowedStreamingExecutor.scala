package com.bwsw.sj.engine.core.windowed

import com.bwsw.sj.common.engine.{StreamingExecutor, StateHandlers}
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager

/**
 * Class is responsible for windowed module execution logic.
 * Module uses a specific instance, to personalize its work.
 * Executor provides following methods, which don't do anything by default so you should define their implementation by yourself
 *
 * @author Kseniya Mikhaleva
 */


class WindowedStreamingExecutor(manager: ModuleEnvironmentManager) extends StreamingExecutor with StateHandlers {
  /**
   * Is invoked only once at the beginning of launching of module
   */
  def onInit(): Unit = {}

  /**
   * Used for processing one envelope. It is invoked for every received message
   * from one of the inputs that are defined within the instance.
   */
  def onWindow(windowRepository: WindowRepository): Unit = {
    //println("onWindow() " + windowRepository.getAll().map(x => (x._1, x._2.batches.size))) //todo
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
}