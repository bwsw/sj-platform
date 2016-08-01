package com.bwsw.sj.engine.regular.task.engine

import com.bwsw.sj.common.utils.SjTimer
import com.bwsw.sj.engine.core.PersistentBlockingQueue
import com.bwsw.sj.engine.regular.task.RegularTaskManager
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics

/**
 * Provides methods are responsible for a basic execution logic of task of regular module
 * that has a checkpoint based on time
 * Created: 26/07/2016
 *
 * @param manager Manager of environment of task of regular module
 * @param performanceMetrics Set of metrics that characterize performance of a regular streaming module
 * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
 *                      which will be retrieved into a module
 * @author Kseniya Mikhaleva
 */
class TimeCheckpointRegularTaskEngine(manager: RegularTaskManager,
                                      performanceMetrics: RegularStreamingPerformanceMetrics,
                                      blockingQueue: PersistentBlockingQueue)
  extends RegularTaskEngine(manager, performanceMetrics, blockingQueue) {

  currentThread.setName(s"regular-task-${manager.taskName}-engine-with-time-checkpoint")
  private val checkpointTimer: Option[SjTimer] = createTimer()
  val isNotOnlyCustomCheckpoint = checkpointTimer.isDefined

  /**
   * Creates a timer for performing checkpoints
   * @return Timer or nothing if instance has no timer and will do checkpoints by manual
   */
  private def createTimer() = {
    if (regularInstance.checkpointInterval > 0) {
      logger.debug(s"Task: ${manager.taskName}. Create a checkpoint timer for input module\n")
      Some(new SjTimer())
    } else {
      logger.debug(s"Task: ${manager.taskName}. Input module has not programmatic checkpoint. Manually only\n")
      None
    }
  }

  /**
   * Sets timer on checkpoint interval
   */
  private def setTimer() = {
    checkpointTimer.get.set(regularInstance.checkpointInterval)
  }

  /**
   * Check whether a group checkpoint of t-streams consumers/producers have to be done or not
   * @param isCheckpointInitiated Flag points whether checkpoint was initiated inside regular module (not on the schedule) or not.
   */
  def isItTimeToCheckpoint(isCheckpointInitiated: Boolean) = {
    isNotOnlyCustomCheckpoint && checkpointTimer.get.isTime || isCheckpointInitiated
  }

  /**
   * Does group checkpoint of t-streams consumers/producers
   */
  override def doCheckpoint() = {
    {
      super.doCheckpoint()
      resetTimer()
    }
  }

  /**
   * Prepares a timer for next circle, e.i. reset a timer and set again
   */
  private def resetTimer() = {
    if (isNotOnlyCustomCheckpoint) {
      logger.debug(s"Task: ${manager.taskName}. Prepare a checkpoint timer for next cycle\n")
      checkpointTimer.get.reset()
      setTimer()
    }
  }
}
