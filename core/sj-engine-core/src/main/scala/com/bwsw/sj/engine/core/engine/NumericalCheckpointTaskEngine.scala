package com.bwsw.sj.engine.core.engine

import com.bwsw.sj.engine.core.environment.EnvironmentManager
import com.bwsw.sj.engine.core.managment.TaskManager
import org.slf4j.LoggerFactory

/**
 * Provides methods for a basic execution logic of task engine that has an every-nth checkpoint mode
 */

trait NumericalCheckpointTaskEngine {

  private val logger = LoggerFactory.getLogger(this.getClass)
  protected val manager: TaskManager
  protected val environmentManager: EnvironmentManager
  private var countOfEnvelopes = 0
  private lazy val checkpointInterval = manager.getCheckpointInterval()
  val isNotOnlyCustomCheckpoint = checkpointInterval > 0

  def isItTimeToCheckpoint(isCheckpointInitiated: Boolean): Boolean = {
    isNotOnlyCustomCheckpoint && countOfEnvelopes == checkpointInterval || environmentManager.isCheckpointInitiated
  }

  def afterReceivingEnvelope() = {
    increaseCounter()
  }

  private def increaseCounter() = {
    logger.debug(s"Task: ${manager.taskName}. Increase count of envelopes to: $countOfEnvelopes\n")
    countOfEnvelopes += 1
  }

  def prepareForNextCheckpoint() = {
    resetCounter()
  }

  private def resetCounter() = {
    logger.debug(s"Task: ${manager.taskName}. Reset a counter of envelopes to 0\n")
    countOfEnvelopes = 0
  }
}
