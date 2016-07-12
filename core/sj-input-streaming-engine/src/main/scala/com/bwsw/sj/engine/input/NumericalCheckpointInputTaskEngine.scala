package com.bwsw.sj.engine.input

import com.bwsw.sj.common.DAL.model.module.InputInstance
import com.bwsw.sj.engine.core.entities.InputEnvelope


class NumericalCheckpointInputTaskEngine(manager: InputTaskManager, inputInstanceMetadata: InputInstance)
  extends InputTaskEngine(manager) {

  private var countOfEnvelopes = 0
  val isNotOnlyCustomCheckpoint = inputInstanceMetadata.checkpointInterval > 0

  def doCheckpoint(isCheckpointInitiated: Boolean) = {
    if (isNotOnlyCustomCheckpoint && countOfEnvelopes == inputInstanceMetadata.checkpointInterval || moduleEnvironmentManager.isCheckpointInitiated) {
      logger.info(s"Task: ${manager.taskName}. It's time to checkpoint\n")
      logger.debug(s"Task: ${manager.taskName}. Do group checkpoint\n")
      checkpointGroup.commit()
      txnsByStreamPartitions.foreach(x => x._2.foreach(y => txnClose(y._2.getTxnUUID)))
      txnsByStreamPartitions = createTxnsStorage()
      logger.debug(s"Task: ${manager.taskName}. Reset a counter of envelopes to 0\n")
      resetCounter()
    }
  }

  override def processEnvelope(envelope: Option[InputEnvelope]) = {
    val isNotDuplicateOrEmpty = super.processEnvelope(envelope)
    if (isNotDuplicateOrEmpty) {
      increaseCounter()
    }

    isNotDuplicateOrEmpty
  }

  private def increaseCounter() = {
    logger.debug(s"Task: ${manager.taskName}. Increase count of envelopes to: $countOfEnvelopes\n")
    countOfEnvelopes += 1
  }

  private def resetCounter() = {
    logger.debug(s"Task: ${manager.taskName}. Reset a counter of envelopes to 0\n")
    countOfEnvelopes = 0
  }
}
