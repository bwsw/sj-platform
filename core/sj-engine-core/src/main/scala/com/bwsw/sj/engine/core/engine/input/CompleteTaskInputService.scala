package com.bwsw.sj.engine.core.engine.input

import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.entities.{Envelope, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import org.slf4j.LoggerFactory

/**
 * Class is responsible for handling kafka inputs and t-stream inputs
 *
 *
 * @author Kseniya Mikhaleva
 *
 * @param manager Manager of environment of task of regular/windowed module
 * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
 *                      which will be retrieved into a module
 */
class CompleteTaskInputService(manager: CommonTaskManager,
                                      blockingQueue: PersistentBlockingQueue)
  extends TaskInputService(manager.inputs) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val kafkaRegularTaskInputService = new KafkaTaskInputService(manager, blockingQueue)
  private val tStreamRegularTaskInputService = new TStreamTaskInputService(manager, blockingQueue)

  override def registerEnvelope(envelope: Envelope, performanceMetrics: PerformanceMetrics) = {
    envelope match {
      case _ : TStreamEnvelope =>
        tStreamRegularTaskInputService.registerEnvelope(envelope, performanceMetrics)
      case _ : KafkaEnvelope =>
        kafkaRegularTaskInputService.registerEnvelope(envelope, performanceMetrics)
      case wrongEnvelope =>
        logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for regular/windowed streaming engine")
        throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for regular/windowed streaming engine")
    }
  }

  def call() = {
    tStreamRegularTaskInputService.call()
    kafkaRegularTaskInputService.call()
  }

  override def setConsumerOffset(envelope: Envelope) = {
    envelope match {
      case _ : TStreamEnvelope =>
        tStreamRegularTaskInputService.setConsumerOffset(envelope)
      case _ : KafkaEnvelope =>
        kafkaRegularTaskInputService.setConsumerOffset(envelope)
      case wrongEnvelope =>
        logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for regular/windowed streaming engine")
        throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for regular/windowed streaming engine")
    }
  }

  override def doCheckpoint() = {
    kafkaRegularTaskInputService.doCheckpoint()
  }
}
