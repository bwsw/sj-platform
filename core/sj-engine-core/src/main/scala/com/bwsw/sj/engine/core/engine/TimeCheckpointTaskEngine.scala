package com.bwsw.sj.engine.core.engine

import com.bwsw.sj.common.utils.SjTimer
import com.bwsw.sj.engine.core.managment.TaskManager
import org.slf4j.LoggerFactory

/**
 * Provides methods for a basic execution logic of task engine that has a checkpoint based on time
 */
trait TimeCheckpointTaskEngine {
  private val logger = LoggerFactory.getLogger(this.getClass)
  protected val manager: TaskManager
  private lazy val checkpointInterval = manager.instance.getCheckpointInterval()

  private val checkpointTimer: Option[SjTimer] = createTimer()
  val isNotOnlyCustomCheckpoint = checkpointTimer.isDefined

  if (isNotOnlyCustomCheckpoint) setTimer()

  private def createTimer() = {
    if (checkpointInterval > 0) {
      logger.debug(s"Task: ${manager.taskName}. Create a checkpoint timer for an input module\n")
      Some(new SjTimer())
    } else {
      logger.debug(s"Task: ${manager.taskName}. Input module has not programmatic checkpoint. Manually only\n")
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
      logger.debug(s"Task: ${manager.taskName}. Prepare a checkpoint timer for next cycle\n")
      checkpointTimer.get.reset()
      setTimer()
    }
  }

  def afterReceivingEnvelope(): Unit = {}
}
