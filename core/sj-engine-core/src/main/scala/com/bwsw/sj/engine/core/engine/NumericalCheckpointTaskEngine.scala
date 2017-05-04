package com.bwsw.sj.engine.core.engine

import org.slf4j.LoggerFactory

/**
 * Provides methods for a basic execution logic of task engine that has an every-nth checkpoint mode
 */

trait NumericalCheckpointTaskEngine {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private var countOfEnvelopes = 0
  protected val checkpointInterval: Long
  private val isNotOnlyCustomCheckpoint = checkpointInterval > 0

  def isItTimeToCheckpoint(isCheckpointInitiated: Boolean): Boolean = {
    isNotOnlyCustomCheckpoint && countOfEnvelopes == checkpointInterval || isCheckpointInitiated
  }

  def afterReceivingEnvelope(): Unit = {
    increaseCounter()
  }

  private def increaseCounter(): Unit = {
    countOfEnvelopes += 1
    logger.debug(s"Increase count of envelopes to: $countOfEnvelopes.")
  }

  def prepareForNextCheckpoint(): Unit = {
    resetCounter()
  }

  private def resetCounter(): Unit = {
    logger.debug(s"Reset a counter of envelopes to 0.")
    countOfEnvelopes = 0
  }
}
