package com.bwsw.sj.engine.windowed.task.engine.collecting

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.engine.core.entities.Transaction

trait TransactionBatchCollecting {
  private var startValue: Long = 0
  private var endValue: Long = 0
  protected val instance: WindowedInstance
  private val transactionInterval = instance.batchFillType.value * 10000

  def isItTimeToCollectBatch(): Boolean = {
    startValue + transactionInterval <= endValue
  }

  def afterReceivingTransaction(transaction: Transaction) = {
    if (startValue == 0) startValue = transaction.id
    endValue = transaction.id
  }

  def prepareForNextBatchCollecting() = {
    startValue = endValue
  }
}
