package com.bwsw.sj.engine.core.engine

import com.bwsw.sj.common.utils.SjTimer
import org.slf4j.LoggerFactory

/**
  * Provides methods for a basic execution logic of task engine
  * that has a checkpoint based on time [[com.bwsw.sj.common.utils.EngineLiterals.timeIntervalMode]]
  */

trait TimeCheckpointTaskEngine {
  private val logger = LoggerFactory.getLogger(this.getClass)
  protected val checkpointInterval: Long

  private val checkpointTimer: Option[SjTimer] = createTimer()
  private val isNotOnlyCustomCheckpoint = checkpointTimer.isDefined

  if (isNotOnlyCustomCheckpoint) setTimer()

  private def createTimer(): Option[SjTimer] = {
    if (checkpointInterval > 0) {
      logger.debug(s"Create a checkpoint timer.")
      Some(new SjTimer())
    } else {
      logger.debug(s"Input module has not programmatic checkpoint. Manually only.")
      None
    }
  }

  private def setTimer(): Unit = {
    checkpointTimer.get.set(checkpointInterval)
  }

  def isItTimeToCheckpoint(isCheckpointInitiated: Boolean): Boolean = {
    isNotOnlyCustomCheckpoint && checkpointTimer.get.isTime || isCheckpointInitiated
  }

  def prepareForNextCheckpoint(): Unit = {
    resetTimer()
  }

  private def resetTimer(): Unit = {
    if (isNotOnlyCustomCheckpoint) {
      logger.debug(s"Prepare a checkpoint timer for next cycle.")
      checkpointTimer.get.reset()
      setTimer()
    }
  }

  def afterReceivingEnvelope(): Unit = {}
}
