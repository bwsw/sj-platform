package com.bwsw.sj.engine.input

import com.bwsw.sj.common.DAL.model.module.InputInstance
import com.bwsw.sj.common.utils.SjTimer

/**
 * Provides methods are responsible for a basic execution logic of task of input module
 * that has a checkpoint based on time
 *
 * @param manager Manager of environment of task of input module
 * @param inputInstanceMetadata Input instance is a metadata for running a task of input module
 */
class TimeCheckpointInputTaskEngine(manager: InputTaskManager, inputInstanceMetadata: InputInstance)
  extends InputTaskEngine(manager, inputInstanceMetadata) {

  private val checkpointTimer: Option[SjTimer] = createTimer()
  val isNotOnlyCustomCheckpoint = checkpointTimer.isDefined

  /**
   * Creates a timer for performing checkpoints
   * @return Timer or nothing if instance has no timer and will do checkpoints by manual
   */
  private def createTimer() = {
    if (inputInstanceMetadata.checkpointInterval > 0) {
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
    checkpointTimer.get.set(inputInstanceMetadata.checkpointInterval)
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
      txnsByStreamPartitions.foreach(x => x._2.foreach(y => txnClose(y._2.getTxnUUID)))
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

