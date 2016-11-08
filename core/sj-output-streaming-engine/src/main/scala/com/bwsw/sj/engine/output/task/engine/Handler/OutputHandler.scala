package com.bwsw.sj.engine.output.task.engine.Handler

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.engine.core.entities.{Envelope, TStreamEnvelope}
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

/**
  * Created by diryavkin_dn on 07.11.16.
  *
  * This class used in OutputTaskEngine for sending data to different storage.
  * Create concrete handler and realize prepare() and send() methods.
  */
abstract class OutputHandler(outputStream: SjStream,
                             performanceMetrics: OutputStreamingPerformanceMetrics,
                             manager: OutputTaskManager) {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  private val byteSerializer = new ObjectSerializer()
  protected val envelopeSerializer = new JsonSerializer()

  /**
    * Main method of handler: prepare, register and send.
    *
    * @param envelopes: list of processed envelopes from user executor.
    * @param inputEnvelope: received envelope
    * @param wasFirstCheckpoint: boolean
    */
  def process(envelopes: List[Envelope], inputEnvelope: TStreamEnvelope, wasFirstCheckpoint: Boolean) = {
    prepare(inputEnvelope, wasFirstCheckpoint)
    envelopes.foreach(envelope => registerAndSendEnvelope(envelope, inputEnvelope))
  }

  /**
    * Method for sending data to storage. Must be realized.
    *
    * @param envelope: processed envelope
    * @param inputEnvelope: received envelope
    */
  def send(envelope: Envelope, inputEnvelope: TStreamEnvelope)

  /**
    * Method for preparing storage environment. Must be realized.
    *
    * @param inputEnvelope: received envelope
    * @param wasFirstCheckpoint: boolean
    */
  def prepare(inputEnvelope: TStreamEnvelope, wasFirstCheckpoint: Boolean)

  /**
    * Registration envelope in performance metrics, and then sending to storage
    *
    * @param outputEnvelope
    * @param inputEnvelope
    */
  def registerAndSendEnvelope(outputEnvelope: Envelope, inputEnvelope: TStreamEnvelope) = {
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
    val elementSize = byteSerializer.serialize(data).length
    performanceMetrics.addElementToOutputEnvelope(outputStream.name, envelopeID, elementSize)
  }
}


