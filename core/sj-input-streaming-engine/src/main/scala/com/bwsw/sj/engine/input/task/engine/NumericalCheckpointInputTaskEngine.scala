package com.bwsw.sj.engine.input.task.engine

import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.input.task.InputTaskManager
import com.bwsw.sj.engine.input.task.reporting.InputStreamingPerformanceMetrics
import io.netty.buffer.ByteBuf

/**
 * Provides methods are responsible for a basic execution logic of task of input module
 * that has an every-nth checkpoint mode
 *
 * @param manager Manager of environment of task of input module
 */
class NumericalCheckpointInputTaskEngine(manager: InputTaskManager,
                                         performanceMetrics: InputStreamingPerformanceMetrics,
                                         buffer: ByteBuf)
  extends InputTaskEngine(manager, performanceMetrics, buffer) {

  private var countOfEnvelopes = 0
  val isNotOnlyCustomCheckpoint = inputInstance.checkpointInterval > 0

  /**
   * Does group checkpoint of t-streams consumers/producers
   * @param isCheckpointInitiated Flag points whether checkpoint was initiated inside input module (not on the schedule) or not.
   */
  def doCheckpoint(isCheckpointInitiated: Boolean) = {
    if (isNotOnlyCustomCheckpoint && countOfEnvelopes == inputInstance.checkpointInterval || moduleEnvironmentManager.isCheckpointInitiated) {
      logger.info(s"Task: ${manager.taskName}. It's time to checkpoint\n")
      logger.debug(s"Task: ${manager.taskName}. Do group checkpoint\n")
      checkpointGroup.commit()
      checkpointInitiated()
      txnsByStreamPartitions = createTxnsStorage(streams)
      logger.debug(s"Task: ${manager.taskName}. Reset a counter of envelopes to 0\n")
      resetCounter()
    }
  }

  /**
   * It is responsible for processing of envelope:
   * 1) does all that a superclass does
   * 2) if an input envelope is processed increase a counter of incoming envelopes
   * @param envelope May be input envelope
   * @return True if a processed envelope is processed, e.i. it is not duplicate or empty, and false in other case
   */
  override def processEnvelope(envelope: Option[InputEnvelope]) = {
    val isNotDuplicateOrEmpty = super.processEnvelope(envelope)
    if (isNotDuplicateOrEmpty) {
      logger.debug(s"Task name: ${manager.taskName}. Processed envelope is not duplicate or empty " +
        s"so increase a counter of input envelopes\n")
      increaseCounter()
    }

    isNotDuplicateOrEmpty
  }

  /**
   * Increases a counter of incoming envelopes
   */
  private def increaseCounter() = {
    logger.debug(s"Task: ${manager.taskName}. Increase count of envelopes to: $countOfEnvelopes\n")
    countOfEnvelopes += 1
  }

  /**
   * Prepares a counter of incoming envelopes for next circle, e.i. reset a counter to 0
   */
  private def resetCounter() = {
    logger.debug(s"Task: ${manager.taskName}. Reset a counter of envelopes to 0\n")
    countOfEnvelopes = 0
  }
}
