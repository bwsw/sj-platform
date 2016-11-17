package com.bwsw.sj.engine.core.engine.input

import java.util.concurrent.Callable

import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory

/**
 * Class is responsible for handling an input streams of specific type(types),
 * i.e. for consuming, processing and sending the input envelopes
 *
 * @author Kseniya Mikhaleva
 */
abstract class TaskInputService[T <: Envelope](inputs: scala.collection.mutable.Map[SjStream, Array[Int]]) {
  private val lastEnvelopesByStreams = createStorageOfLastEnvelopes()
  val checkpointGroup: CheckpointGroup

  private def createStorageOfLastEnvelopes() = {
    inputs.flatMap(x => x._2.map(y => ((x._1.name, y), new Envelope())))
  }

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

abstract class CallableTaskInputService[T <: Envelope](inputs: scala.collection.mutable.Map[SjStream, Array[Int]]) extends TaskInputService[T](inputs) with Callable[Unit]

object CallableTaskInputService {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(manager: CommonTaskManager, blockingQueue: PersistentBlockingQueue) = {
    val isKafkaInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.kafkaStreamType)
    val isTstreamInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.tStreamType)

    (isKafkaInputExist, isTstreamInputExist) match {
      case (true, true) => new CompleteTaskInputService(manager, blockingQueue)
      case (false, true) => new TStreamTaskInputService(manager, blockingQueue)
      case (true, false) => new KafkaTaskInputService(manager, blockingQueue)
      case _ =>
        logger.error("Type of input stream is not 'kafka' or 't-stream'")
        throw new Exception("Type of input stream is not 'kafka' or 't-stream'")
    }
  }
}