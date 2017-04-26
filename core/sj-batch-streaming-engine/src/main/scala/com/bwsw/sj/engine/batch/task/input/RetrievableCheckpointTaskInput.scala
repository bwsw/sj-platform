package com.bwsw.sj.engine.batch.task.input

import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.engine.core.engine.input.CheckpointTaskInput
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
 * Class is responsible for handling an input streams of specific type(types),
 * i.e. for consuming, processing and sending the input envelopes
 *
 * @author Kseniya Mikhaleva
 */
abstract class RetrievableCheckpointTaskInput[T <: Envelope](val inputs: scala.collection.mutable.Map[SjStream, Array[Int]]) extends CheckpointTaskInput() {
  private val lastEnvelopesByStreams: mutable.Map[(String, Int), Envelope] = createStorageOfLastEnvelopes()

  private def createStorageOfLastEnvelopes() = {
    inputs.flatMap(x => x._2.map(y => ((x._1.name, y), new Envelope())))
  }

  def registerEnvelope(envelope: T) = {
    lastEnvelopesByStreams((envelope.stream, envelope.partition)) = envelope
  }

  def setConsumerOffsetToLastEnvelope() = {
    lastEnvelopesByStreams.values.filterNot(_.isEmpty()).foreach(envelope => {
      setConsumerOffset(envelope.asInstanceOf[T])
    })
    lastEnvelopesByStreams.clear()
  }

  protected def setConsumerOffset(envelope: T)

  override def doCheckpoint(): Unit = {
    setConsumerOffsetToLastEnvelope()
    super.doCheckpoint()
  }

  def get(): Iterable[T]
}

object RetrievableCheckpointTaskInput {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply[T <: AnyRef](manager: CommonTaskManager) = {
    val isKafkaInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.kafkaStreamType)
    val isTstreamInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.tstreamType)

    (isKafkaInputExist, isTstreamInputExist) match {
      case (true, true) => new RetrievableCompleteCheckpointTaskInput[T](manager)
      case (false, true) => new RetrievableTStreamCheckpointTaskInput[T](manager)
      case (true, false) => new RetrievableKafkaCheckpointTaskInput[T](manager)
      case _ =>
        logger.error("Type of input stream is not 'kafka' or 't-stream'")
        throw new RuntimeException("Type of input stream is not 'kafka' or 't-stream'")
    }
  }
}