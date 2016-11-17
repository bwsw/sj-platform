package com.bwsw.sj.engine.windowed.task.input

import com.bwsw.sj.engine.core.entities.{Envelope, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory

/**
 * Class is responsible for handling kafka input and t-stream input
 *
 * @author Kseniya Mikhaleva
 */
class CompleteTaskInput(manager: CommonTaskManager) extends {
  override val checkpointGroup = new CheckpointGroup()
} with TaskInput[Envelope](manager.inputs) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val kafkaRegularTaskInputService = new KafkaTaskInput(manager, checkpointGroup)
  private val tStreamRegularTaskInputService = new TStreamTaskInput(manager, checkpointGroup)

  override def registerEnvelope(envelope: Envelope) = {
    envelope match {
      case tstreamEnvelope: TStreamEnvelope =>
        tStreamRegularTaskInputService.registerEnvelope(tstreamEnvelope)
      case kafkaEnvelope: KafkaEnvelope =>
        kafkaRegularTaskInputService.registerEnvelope(kafkaEnvelope)
      case wrongEnvelope =>
        logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for windowed streaming engine")
        throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for windowed streaming engine")
    }
  }

  override def get() = {
    kafkaRegularTaskInputService.get() ++ tStreamRegularTaskInputService.get()
  }

  override def setConsumerOffsetToLastEnvelope() = {
    tStreamRegularTaskInputService.setConsumerOffsetToLastEnvelope()
    kafkaRegularTaskInputService.setConsumerOffsetToLastEnvelope()
  }

  override def setConsumerOffset(envelope: Envelope) = {
    envelope match {
      case tstreamEnvelope: TStreamEnvelope =>
        tStreamRegularTaskInputService.setConsumerOffset(tstreamEnvelope)
      case kafkaEnvelope: KafkaEnvelope =>
        kafkaRegularTaskInputService.setConsumerOffset(kafkaEnvelope)
      case wrongEnvelope =>
        logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for windowed streaming engine")
        throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for windowed streaming engine")
    }
  }
}
