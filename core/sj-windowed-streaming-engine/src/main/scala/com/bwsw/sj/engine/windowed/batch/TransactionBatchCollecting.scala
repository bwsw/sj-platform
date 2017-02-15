package com.bwsw.sj.engine.windowed.batch

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.engine.core.entities.Envelope

trait TransactionBatchCollecting {
  private var startValue: Long = 0
  private var endValue: Long = 0
  protected val instance: WindowedInstance
  private val transactionInterval = instance.batchFillType.value * 10000

  def isItTimeToCollectBatch(): Boolean = {
    startValue + transactionInterval <= endValue
  }

  def afterReceivingEnvelope(envelope: Envelope) = {
    if (startValue == 0) startValue = envelope.id
    endValue = envelope.id
  }

  def prepareForNextBatchCollecting() = {
    startValue = endValue
  }
}
