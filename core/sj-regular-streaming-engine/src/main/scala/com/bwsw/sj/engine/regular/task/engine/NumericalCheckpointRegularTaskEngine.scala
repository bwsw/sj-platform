package com.bwsw.sj.engine.regular.task.engine

import com.bwsw.sj.engine.core.PersistentBlockingQueue
import com.bwsw.sj.engine.regular.task.RegularTaskManager
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics

/**
 * Provides methods are responsible for a basic execution logic of task of regular module
 * that has an every-nth checkpoint mode
 * Created: 26/07/2016
 *
 * @param manager Manager of environment of task of regular module
 * @param performanceMetrics Set of metrics that characterize performance of a regular streaming module
 * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
 *                      which will be retrieved into a module
 * @author Kseniya Mikhaleva
 */
class NumericalCheckpointRegularTaskEngine(manager: RegularTaskManager,
                                           performanceMetrics: RegularStreamingPerformanceMetrics,
                                           blockingQueue: PersistentBlockingQueue)
  extends RegularTaskEngine(manager, performanceMetrics, blockingQueue) {

  currentThread.setName(s"regular-task-${manager.taskName}-engine-with-numerical-checkpoint")
  private var countOfEnvelopes = 0
  val isNotOnlyCustomCheckpoint: Boolean = regularInstance.checkpointInterval > 0

  /**
   * Check whether a group checkpoint of t-streams consumers/producers have to be done or not
   * @param isCheckpointInitiated Flag points whether checkpoint was initiated inside regular module (not on the schedule) or not.
   */
  def isItTimeToCheckpoint(isCheckpointInitiated: Boolean) = {
    isNotOnlyCustomCheckpoint && countOfEnvelopes == regularInstance.checkpointInterval || isCheckpointInitiated
  }

  /**
   * Does group checkpoint of t-streams consumers/producers
   */
  override def doCheckpoint() = {
    {
      super.doCheckpoint()
      resetCounter()
    }
  }

  /**
   * It is responsible for handling an event about an envelope has been received
   */
  override def afterReceivingEnvelope() = {
    increaseCounter()
  }

  /**
   * Increases a counter of incoming envelopes
   */
  private def increaseCounter() = {
    logger.debug(s"Task: ${manager.taskName}. Increase count of envelopes to: $countOfEnvelopes\n")
    countOfEnvelopes += 1
  }

  /**
   * Prepares a counter of incoming envelopes for next circle, e.i. reset a counter to 0
   */
  private def resetCounter() = {
    logger.debug(s"Task: ${manager.taskName}. Reset a counter of envelopes to 0\n")
    countOfEnvelopes = 0
  }

}
