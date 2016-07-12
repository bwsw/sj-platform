package com.bwsw.sj.engine.input

import com.bwsw.sj.common.DAL.model.module.InputInstance
import com.bwsw.sj.common.utils.SjTimer

class TimeCheckpointInputTaskEngine(manager: InputTaskManager, inputInstanceMetadata: InputInstance)
  extends InputTaskEngine(manager) {

  private val checkpointTimer: Option[SjTimer] = createTimer()
  val isNotOnlyCustomCheckpoint = checkpointTimer.isDefined

  private def createTimer() = {
    if (inputInstanceMetadata.checkpointInterval > 0) {
      Some(new SjTimer())
    } else None
  }

  private def setTimer() = {
    checkpointTimer.get.set(inputInstanceMetadata.checkpointInterval)
  }

  def doCheckpoint(isCheckpointInitiated: Boolean) = {
    if (isNotOnlyCustomCheckpoint && checkpointTimer.get.isTime || isCheckpointInitiated) {
      logger.info(s"Task: ${manager.taskName}. It's time to checkpoint\n")
      logger.debug(s"Task: ${manager.taskName}. Do group checkpoint\n")
      checkpointGroup.commit()
      txnsByStreamPartitions.foreach(x => x._2.foreach(y => txnClose(y._2.getTxnUUID)))
      txnsByStreamPartitions = createTxnsStorage()
      resetTimer()
    }
  }

  private def resetTimer() = {
    if (checkpointTimer.isDefined) {
      logger.debug(s"Task: ${manager.taskName}. Prepare a checkpoint timer for next cycle\n")
      checkpointTimer.get.reset()
      setTimer()
    }
  }
}

