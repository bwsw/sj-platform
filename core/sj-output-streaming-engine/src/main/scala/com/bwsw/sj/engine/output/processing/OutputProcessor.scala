package com.bwsw.sj.engine.output.processing

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.engine.core.entities.{Envelope, TStreamEnvelope}
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

/**
 * This class used in OutputTaskEngine for sending data to different storage.
 * Create concrete handler and realize remove() and send() methods.
 */
abstract class OutputProcessor(outputStream: SjStream,
                               performanceMetrics: OutputStreamingPerformanceMetrics) {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  private val byteSerializer = new ObjectSerializer()

  /**
   * Main method of handler: prepare, register and send.
   *
   * @param envelopes: list of processed envelopes from user executor.
   * @param inputEnvelope: received envelope
   * @param wasFirstCheckpoint: boolean
   */
  def process(envelopes: List[Envelope], inputEnvelope: TStreamEnvelope, wasFirstCheckpoint: Boolean) = {
    logger.debug("Process a set of envelopes that should be sent to output of specific type.")
    if (!wasFirstCheckpoint) remove(inputEnvelope)
    envelopes.foreach(envelope => registerAndSendEnvelope(envelope, inputEnvelope))
  }

  def remove(envelope: TStreamEnvelope)

  def close()

  /**
   * Registration envelope in performance metrics, and then sending to storage
   */
  private def registerAndSendEnvelope(outputEnvelope: Envelope, inputEnvelope: TStreamEnvelope) = {
    registerOutputEnvelope(inputEnvelope.id.toString.replaceAll("-", ""), outputEnvelope)
    send(outputEnvelope, inputEnvelope)
  }

  /**
   * Register processed envelope in performance metrics.
   *
   * @param envelopeID: envelope identifier
   * @param data: processed envelope
   */
  private def registerOutputEnvelope(envelopeID: String, data: Envelope) = {
    logger.debug(s"Register an output envelope: '$envelopeID'.")
    val elementSize = byteSerializer.serialize(data).length
    performanceMetrics.addElementToOutputEnvelope(outputStream.name, envelopeID, elementSize)
  }

  /**
   * Method for sending data to storage. Must be realized.
   *
   * @param envelope: processed envelope
   * @param inputEnvelope: received envelope
   */
  protected def send(envelope: Envelope, inputEnvelope: TStreamEnvelope)
}

object OutputProcessor {
  def apply(outputStream: SjStream, performanceMetrics: OutputStreamingPerformanceMetrics, manager: OutputTaskManager) = {
    outputStream.streamType match {
      case StreamLiterals.esOutputType =>
        new EsOutputProcessor(outputStream, performanceMetrics, manager)
      case StreamLiterals.jdbcOutputType =>
        new JdbcOutputProcessor(outputStream, performanceMetrics)
    }
  }
}
