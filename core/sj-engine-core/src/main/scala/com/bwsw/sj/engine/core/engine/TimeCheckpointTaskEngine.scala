package com.bwsw.sj.engine.core.engine

import com.bwsw.sj.common.utils.SjTimer
import org.slf4j.LoggerFactory

/**
 * Provides methods for a basic execution logic of task engine that has a checkpoint based on time
 */
trait TimeCheckpointTaskEngine {
  private val logger = LoggerFactory.getLogger(this.getClass)
  protected val checkpointInterval: Long

  private val checkpointTimer: Option[SjTimer] = createTimer()
  private val isNotOnlyCustomCheckpoint = checkpointTimer.isDefined

  if (isNotOnlyCustomCheckpoint) setTimer()

  private def createTimer() = {
    if (checkpointInterval > 0) {
      logger.debug(s"Create a checkpoint timer for an input module\n")
      Some(new SjTimer())
    } else {
      logger.debug(s"Input module has not programmatic checkpoint. Manually only\n")
      None
    }
  }

  private def setTimer() = {
    checkpointTimer.get.set(checkpointInterval)
  }

  def isItTimeToCheckpoint(isCheckpointInitiated: Boolean): Boolean = {
    isNotOnlyCustomCheckpoint && checkpointTimer.get.isTime || isCheckpointInitiated
  }

  def prepareForNextCheckpoint() = {
    resetTimer()
  }

  private def resetTimer() = {
    if (isNotOnlyCustomCheckpoint) {
      logger.debug(s"Prepare a checkpoint timer for next cycle\n")
      checkpointTimer.get.reset()
      setTimer()
    }
  }

  def afterReceivingEnvelope(): Unit = {}
}
