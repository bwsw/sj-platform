package com.bwsw.sj.engine.windowed.task.engine.collecting

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.engine.core.entities.Transaction
import org.slf4j.LoggerFactory

trait NumericalBatchCollecting {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private var countOfEnvelopes = 0
  protected val instance: WindowedInstance

  def isItTimeToCollectBatch(): Boolean = {
    countOfEnvelopes == instance.batchFillType.value
  }

  def afterReceivingTransaction(transaction: Transaction) = {
    increaseCounter()
  }

  private def increaseCounter() = {
    logger.debug(s"Increase count of envelopes to: $countOfEnvelopes\n")
    countOfEnvelopes += 1
  }

  def prepareForNextBatchCollecting() = {
    resetCounter()
  }

  private def resetCounter() = {
    logger.debug(s"Reset a counter of envelopes to 0\n")
    countOfEnvelopes = 0
  }
}
