package com.bwsw.sj.engine.core.engine.input

import com.bwsw.sj.common.dal.model.stream.SjStream
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.tstreams.agents.group.CheckpointGroup

import scala.collection.mutable

/**
  *  Task input that is able to do checkpoint of consuming or processing messages
  *
  * @author Kseniya Mikhaleva
  */
abstract class CheckpointTaskInput[E <: Envelope](inputs: scala.collection.mutable.Map[SjStream, Array[Int]]){
  private val lastEnvelopesByStreams: mutable.Map[(String, Int), Envelope] = createStorageOfLastEnvelopes()
  val checkpointGroup: CheckpointGroup

  private def createStorageOfLastEnvelopes() = {
    inputs.flatMap(x => x._2.map(y => ((x._1.name, y), new Envelope())))
  }

  def registerEnvelope(envelope: E) = {
    lastEnvelopesByStreams((envelope.stream, envelope.partition)) = envelope
  }

  def setConsumerOffsetToLastEnvelope() = {
    lastEnvelopesByStreams.values.filterNot(_.isEmpty()).foreach(envelope => {
      setConsumerOffset(envelope.asInstanceOf[E])
    })
    lastEnvelopesByStreams.clear()
  }

  protected def setConsumerOffset(envelope: E)

  def doCheckpoint() = {
    setConsumerOffsetToLastEnvelope()
    checkpointGroup.checkpoint()
  }
}