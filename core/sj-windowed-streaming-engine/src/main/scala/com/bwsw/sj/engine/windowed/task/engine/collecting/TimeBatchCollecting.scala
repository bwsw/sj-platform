package com.bwsw.sj.engine.windowed.task.engine.collecting

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.common.utils.SjTimer
import com.bwsw.sj.engine.core.entities.Transaction
import org.slf4j.LoggerFactory

trait TimeBatchCollecting {
  private val logger = LoggerFactory.getLogger(this.getClass)
  protected val instance: WindowedInstance

  private val batchCollectingTimer = createTimer()
  setTimer()

  private def createTimer() = {
    new SjTimer()
  }

  private def setTimer() = {
    batchCollectingTimer.set(instance.batchFillType.value)
  }

  def isItTimeToCollectBatch(): Boolean = {
    batchCollectingTimer.isTime
  }

  def prepareForNextBatchCollecting() = {
    resetTimer()
  }

  private def resetTimer() = {
    logger.debug(s"Prepare a batch collecting timer for next cycle\n")
    batchCollectingTimer.reset()
    setTimer()
  }

  def afterReceivingTransaction(transaction: Transaction) = {}
}
