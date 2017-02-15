package com.bwsw.sj.engine.core.engine.input

import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
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
class CallableCompleteTaskInput[T](manager: CommonTaskManager,
                                   blockingQueue: PersistentBlockingQueue,
                                   override val checkpointGroup: CheckpointGroup =  new CheckpointGroup()) extends CallableTaskInput[Envelope](manager.inputs) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val kafkaRegularTaskInputService = new CallableKafkaTaskInput[T](manager, blockingQueue, checkpointGroup)
  private val tStreamRegularTaskInputService = new CallableTStreamTaskInput[T](manager, blockingQueue, checkpointGroup)

  override def registerEnvelope(envelope: Envelope) = {
    logger.debug(s"Register an envelope: ${envelope.toString}")
    envelope match {
      case tstreamEnvelope: TStreamEnvelope[T] =>
        tStreamRegularTaskInputService.registerEnvelope(tstreamEnvelope)
      case kafkaEnvelope: KafkaEnvelope[T] =>
        kafkaRegularTaskInputService.registerEnvelope(kafkaEnvelope)
      case wrongEnvelope =>
        logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for regular/windowed streaming engine")
        throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for regular/windowed streaming engine")
    }
  }

  def call() = {
    tStreamRegularTaskInputService.call()
    kafkaRegularTaskInputService.call()
  }

  override def setConsumerOffsetToLastEnvelope() = {
    tStreamRegularTaskInputService.setConsumerOffsetToLastEnvelope()
    kafkaRegularTaskInputService.setConsumerOffsetToLastEnvelope()
  }

  override def setConsumerOffset(envelope: Envelope) = {
    envelope match {
      case tstreamEnvelope: TStreamEnvelope[T] =>
        tStreamRegularTaskInputService.setConsumerOffset(tstreamEnvelope)
      case kafkaEnvelope: KafkaEnvelope[T] =>
        kafkaRegularTaskInputService.setConsumerOffset(kafkaEnvelope)
      case wrongEnvelope =>
        logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for regular/windowed streaming engine")
        throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for regular/windowed streaming engine")
    }
  }

  override def close(): Unit = {
    tStreamRegularTaskInputService.close()
    kafkaRegularTaskInputService.close()
  }
}
