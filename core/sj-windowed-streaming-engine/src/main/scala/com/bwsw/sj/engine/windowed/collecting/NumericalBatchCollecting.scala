package com.bwsw.sj.engine.windowed.collecting

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.engine.core.entities.Envelope
import org.slf4j.LoggerFactory

trait NumericalBatchCollecting {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private var countOfEnvelopes = 0
  protected val instance: WindowedInstance

  def isItTimeToCollectBatch(): Boolean = {
    countOfEnvelopes == instance.batchFillType.value
  }

  def afterReceivingEnvelope(envelope: Envelope) = {
    increaseCounter()
  }

  private def increaseCounter() = {
    logger.debug(s"Increase count of envelopes to: $countOfEnvelopes.")
    countOfEnvelopes += 1
  }

  def prepareForNextBatchCollecting() = {
    resetCounter()
  }

  private def resetCounter() = {
    logger.debug(s"Reset a counter of envelopes to 0.")
    countOfEnvelopes = 0
  }
}
