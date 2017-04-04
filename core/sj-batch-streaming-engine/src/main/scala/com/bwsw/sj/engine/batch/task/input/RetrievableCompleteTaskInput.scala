package com.bwsw.sj.engine.batch.task.input

import com.bwsw.sj.engine.core.entities.{Envelope, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory
import scala.reflect.runtime.universe._

/**
  * Class is responsible for handling kafka input and t-stream input
  *
  * @author Kseniya Mikhaleva
  */
class RetrievableCompleteTaskInput[T <: AnyRef](manager: CommonTaskManager) extends {
  override val checkpointGroup = new CheckpointGroup()
} with RetrievableTaskInput[Envelope](manager.inputs) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val kafkaRegularTaskInputService = new RetrievableKafkaTaskInput[T](manager, checkpointGroup)
  private val tStreamRegularTaskInputService = new RetrievableTStreamTaskInput[T](manager, checkpointGroup)

  override def registerEnvelope(envelope: Envelope) = {
    envelope match {
      case tstreamEnvelope: TStreamEnvelope[T] =>
        tStreamRegularTaskInputService.registerEnvelope(tstreamEnvelope)
      case kafkaEnvelope: KafkaEnvelope[T] =>
        kafkaRegularTaskInputService.registerEnvelope(kafkaEnvelope)
      case wrongEnvelope =>
        logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for batch streaming engine")
        throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for batch streaming engine")
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
      case tstreamEnvelope: TStreamEnvelope[T] =>
        tStreamRegularTaskInputService.setConsumerOffset(tstreamEnvelope)
      case kafkaEnvelope: KafkaEnvelope[T] =>
        kafkaRegularTaskInputService.setConsumerOffset(kafkaEnvelope)
      case wrongEnvelope =>
        logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for batch streaming engine")
        throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for batch streaming engine")
    }
  }
}
