package com.bwsw.sj.engine.core.engine.input

import java.util.concurrent.Callable

import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup

/**
 * Class is responsible for handling an input streams of specific type(types),
 * i.e. for consuming, processing and sending the input envelopes
 *
 * @author Kseniya Mikhaleva
 */
abstract class TaskInputService(inputs: scala.collection.mutable.Map[SjStream, Array[Int]]) extends Callable[Unit] {
  protected val lastEnvelopesByStreams = createStorageOfLastEnvelopes()
  val checkpointGroup = new CheckpointGroup()

  private def createStorageOfLastEnvelopes() = {
    inputs.flatMap(x => x._2.map(y => ((x._1.name, y), new Envelope())))
  }

  def registerEnvelope(envelope: Envelope, performanceMetrics: PerformanceMetrics) = {
    lastEnvelopesByStreams((envelope.stream, envelope.partition)) = envelope
  }

  protected def setConsumerOffsetToLastEnvelope() = {
    lastEnvelopesByStreams.values.filterNot(_.isEmpty()).foreach(envelope => {
      setConsumerOffset(envelope)
    })
  }

  protected def setConsumerOffset(envelope: Envelope)

  def doCheckpoint() = {
    checkpointGroup.checkpoint()
  }
}