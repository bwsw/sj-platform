package com.bwsw.sj.engine.output.processing

import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.Entity
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

/**
  * This class used in OutputTaskEngine for sending data to different storage.
  * Create concrete handler and realize remove() and send() methods.
  */
abstract class OutputProcessor[T <: AnyRef](outputStream: SjStream,
                                            performanceMetrics: OutputStreamingPerformanceMetrics) {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected def transactionFieldName = "txn"

  /**
    * Main method of handler: prepare, register and send.
    *
    * @param envelopes          : list of processed envelopes from user executor.
    * @param inputEnvelope      : received envelope
    * @param wasFirstCheckpoint : boolean
    */
  def process(envelopes: Seq[OutputEnvelope], inputEnvelope: TStreamEnvelope[T], wasFirstCheckpoint: Boolean) = {
    logger.debug("Process a set of envelopes that should be sent to output of specific type.")
    if (!wasFirstCheckpoint) delete(inputEnvelope)
    envelopes.foreach(envelope => registerAndSendEnvelope(envelope, inputEnvelope))
  }

  def delete(envelope: TStreamEnvelope[T])

  def close()

  /**
    * Registration envelope in performance metrics, and then sending to storage
    */
  private def registerAndSendEnvelope(outputEnvelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[T]) = {
    registerOutputEnvelope(inputEnvelope.id.toString.replaceAll("-", ""), outputEnvelope)
    send(outputEnvelope, inputEnvelope)
  }

  /**
    * Register processed envelope in performance metrics.
    *
    * @param envelopeID : envelope identifier
    * @param data       : processed envelope
    */
  private def registerOutputEnvelope(envelopeID: String, data: OutputEnvelope) = {
    logger.debug(s"Register an output envelope: '$envelopeID'.")
    val elementSize = data.toString.length //todo придумать другой способ извлечения информации
    performanceMetrics.addElementToOutputEnvelope(outputStream.name, envelopeID, elementSize)
  }

  /**
    * Method for sending data to storage. Must be realized.
    *
    * @param envelope      : processed envelope
    * @param inputEnvelope : received envelope
    */
  protected def send(envelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[T])
}

object OutputProcessor {
  def apply[T <: AnyRef](outputStream: SjStream, performanceMetrics: OutputStreamingPerformanceMetrics, manager: OutputTaskManager, entity: Entity[_]) = {
    outputStream.streamType match {
      case StreamLiterals.esOutputType =>
        new EsOutputProcessor[T](outputStream, performanceMetrics, manager, entity)
      case StreamLiterals.jdbcOutputType =>
        new JdbcOutputProcessor[T](outputStream, performanceMetrics, manager, entity)
      case StreamLiterals.restOutputType =>
        new RestOutputProcessor[T](outputStream, performanceMetrics, manager, entity)
    }
  }
}
