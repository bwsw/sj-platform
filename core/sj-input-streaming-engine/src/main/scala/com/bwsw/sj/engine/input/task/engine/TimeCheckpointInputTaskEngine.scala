package com.bwsw.sj.engine.input.task.engine

import com.bwsw.sj.common.utils.SjTimer
import com.bwsw.sj.engine.input.task.InputTaskManager
import com.bwsw.sj.engine.input.task.reporting.InputStreamingPerformanceMetrics
import io.netty.buffer.ByteBuf

/**
 * Provides methods are responsible for a basic execution logic of task of input module
 * that has a checkpoint based on time
 *
 * @param manager Manager of environment of task of input module
 */
class TimeCheckpointInputTaskEngine(manager: InputTaskManager,
                                    performanceMetrics: InputStreamingPerformanceMetrics,
                                    buffer: ByteBuf)
  extends InputTaskEngine(manager, performanceMetrics, buffer) {

  private val checkpointTimer: Option[SjTimer] = createTimer()
  val isNotOnlyCustomCheckpoint = checkpointTimer.isDefined

  /**
   * Creates a timer for performing checkpoints
   * @return Timer or nothing if instance has no timer and will do checkpoints by manual
   */
  private def createTimer() = {
    if (inputInstance.checkpointInterval > 0) {
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
    checkpointTimer.get.set(inputInstance.checkpointInterval)
  }

  /**
   * Does group checkpoint of t-streams consumers/producers
   * @param isCheckpointInitiated Flag points whether checkpoint was initiated inside input module (not on the schedule) or not.
   */
  def doCheckpoint(isCheckpointInitiated: Boolean) = {
    if (isNotOnlyCustomCheckpoint && checkpointTimer.get.isTime || isCheckpointInitiated) {
      logger.info(s"Task: ${manager.taskName}. It's time to checkpoint\n")
      logger.debug(s"Task: ${manager.taskName}. Do group checkpoint\n")
      checkpointGroup.commit()
      checkpointInitiated()
      txnsByStreamPartitions = createTxnsStorage(streams)
      resetTimer()
    }
  }

  /**
   * Prepares a timer for next circle, e.i. reset a timer and set again
   */
  private def resetTimer() = {
    if (checkpointTimer.isDefined) {
      logger.debug(s"Task: ${manager.taskName}. Prepare a checkpoint timer for next cycle\n")
      checkpointTimer.get.reset()
      setTimer()
    }
  }
}

