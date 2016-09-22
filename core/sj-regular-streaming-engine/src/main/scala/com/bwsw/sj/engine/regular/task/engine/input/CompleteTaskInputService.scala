package com.bwsw.sj.engine.regular.task.engine.input

import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.engine.input.{TStreamTaskInputService, TaskInputService}
import com.bwsw.sj.engine.core.entities.{Envelope, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.engine.regular.task.RegularTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory

/**
 * Class is responsible for handling kafka inputs and t-stream inputs
 *
 *
 * @author Kseniya Mikhaleva
 *
 * @param manager Manager of environment of task of regular module
 * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
 *                      which will be retrieved into a module
 * @param checkpointGroup Group of t-stream agents that have to make a checkpoint at the same time
 */
class CompleteTaskInputService(manager: RegularTaskManager,
                                      blockingQueue: PersistentBlockingQueue,
                                      checkpointGroup: CheckpointGroup)
  extends TaskInputService {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val kafkaRegularTaskInputService = new KafkaTaskInputService(manager, blockingQueue, checkpointGroup)
  private val tStreamRegularTaskInputService = new TStreamTaskInputService(manager, blockingQueue, checkpointGroup)

  def registerEnvelope(envelope: Envelope, performanceMetrics: PerformanceMetrics) = {
    envelope match {
      case _ : TStreamEnvelope =>
        tStreamRegularTaskInputService.registerEnvelope(envelope, performanceMetrics)
      case _ : KafkaEnvelope =>
        kafkaRegularTaskInputService.registerEnvelope(envelope, performanceMetrics)
      case envelope =>
        logger.error(s"Incoming envelope with type: ${envelope.getClass} is not defined for regular streaming engine")
        throw new Exception(s"Incoming envelope with type: ${envelope.getClass} is not defined for regular streaming engine")
    }
  }

  def call() = {
    tStreamRegularTaskInputService.call()
    kafkaRegularTaskInputService.call()
  }

  override def doCheckpoint() = kafkaRegularTaskInputService.doCheckpoint()
}
