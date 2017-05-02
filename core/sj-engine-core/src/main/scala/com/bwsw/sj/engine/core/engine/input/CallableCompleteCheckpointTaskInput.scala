package com.bwsw.sj.engine.core.engine.input

import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.engine.core.entities.{Envelope, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory

/**
  * Class is responsible for handling kafka inputs and t-stream inputs
  *
  * @author Kseniya Mikhaleva
  * @param manager       Manager of environment of task of regular/windowed module
  * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
  *                      which will be retrieved into a module
  */
class CallableCompleteCheckpointTaskInput[T <: AnyRef](manager: CommonTaskManager,
                                                       blockingQueue: ArrayBlockingQueue[Envelope],
                                                       override val checkpointGroup: CheckpointGroup =  new CheckpointGroup()) extends CallableCheckpointTaskInput[Envelope](manager.inputs) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val kafkaCheckpointTaskInput = new CallableKafkaCheckpointTaskInput[T](manager, blockingQueue, checkpointGroup)
  private val tStreamCheckpointTaskInput = new CallableTStreamCheckpointTaskInput[T](manager, blockingQueue, checkpointGroup)

  override def registerEnvelope(envelope: Envelope) = {
    logger.debug(s"Register an envelope: ${envelope.toString}")
    envelope match {
      case tstreamEnvelope: TStreamEnvelope[T] =>
        tStreamCheckpointTaskInput.registerEnvelope(tstreamEnvelope)
      case kafkaEnvelope: KafkaEnvelope[T] =>
        kafkaCheckpointTaskInput.registerEnvelope(kafkaEnvelope)
      case wrongEnvelope =>
        logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for regular/windowed streaming engine")
        throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for regular/windowed streaming engine")
    }
  }

  def call() = {
    tStreamCheckpointTaskInput.call()
    kafkaCheckpointTaskInput.call()
  }

  override def setConsumerOffsetToLastEnvelope() = {
    tStreamCheckpointTaskInput.setConsumerOffsetToLastEnvelope()
    kafkaCheckpointTaskInput.setConsumerOffsetToLastEnvelope()
  }

  override def setConsumerOffset(envelope: Envelope) = {
    envelope match {
      case tstreamEnvelope: TStreamEnvelope[T] =>
        tStreamCheckpointTaskInput.setConsumerOffset(tstreamEnvelope)
      case kafkaEnvelope: KafkaEnvelope[T] =>
        kafkaCheckpointTaskInput.setConsumerOffset(kafkaEnvelope)
      case wrongEnvelope =>
        logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for regular/windowed streaming engine")
        throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for regular/windowed streaming engine")
    }
  }

  override def close(): Unit = {
    tStreamCheckpointTaskInput.close()
    kafkaCheckpointTaskInput.close()
  }
}
