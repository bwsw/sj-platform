package com.bwsw.sj.engine.output.task.engine.Handler

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.engine.core.entities.{Envelope, TStreamEnvelope}
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

/**
  * Created by diryavkin_dn on 07.11.16.
  */
abstract class OutputHandler(outputStream: SjStream,
                             performanceMetrics: OutputStreamingPerformanceMetrics,
                             manager: OutputTaskManager) {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  private val byteSerializer = new ObjectSerializer()
  protected val envelopeSerializer = new JsonSerializer()

  def process(envelopes: List[Envelope], inputEnvelope: TStreamEnvelope, wasFirstCheckpoint: Boolean) = {
    prepare(inputEnvelope, wasFirstCheckpoint)
    envelopes.foreach(envelope => registerAndSendEnvelope(envelope, inputEnvelope))
  }

  def send(envelope: Envelope, inputEnvelope: TStreamEnvelope)
  def prepare(inputEnvelope: TStreamEnvelope, wasFirstCheckpoint: Boolean)

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


