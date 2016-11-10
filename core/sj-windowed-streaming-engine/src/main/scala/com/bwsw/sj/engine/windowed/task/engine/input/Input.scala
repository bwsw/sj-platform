package com.bwsw.sj.engine.windowed.task.engine.input

import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.tstreams.agents.group.CheckpointGroup

/**
 * Class is responsible for handling an input streams of specific type(types),
 * i.e. for consuming, processing and sending the input envelopes
 *
 * @author Kseniya Mikhaleva
 */
abstract class Input[T <: Envelope](inputs: scala.collection.mutable.Map[SjStream, Array[Int]]) {
  private val lastEnvelopesByStreams = createStorageOfLastEnvelopes()
  val checkpointGroup: CheckpointGroup

  private def createStorageOfLastEnvelopes() = {
    inputs.flatMap(x => x._2.map(y => ((x._1.name, y), new Envelope())))
  }

  def get(): Iterable[T]

  def registerEnvelope(envelope: Envelope) = {
    lastEnvelopesByStreams((envelope.stream, envelope.partition)) = envelope
  }

  def setConsumerOffsetToLastEnvelope() = {
    lastEnvelopesByStreams.values.filterNot(_.isEmpty()).foreach(envelope => {
      setConsumerOffset(envelope.asInstanceOf[T])
    })
    lastEnvelopesByStreams.clear()
  }

  protected def setConsumerOffset(envelope: T)

  def doCheckpoint() = {
    setConsumerOffsetToLastEnvelope()
    checkpointGroup.checkpoint()
  }
}
