package com.bwsw.sj.engine.regular.task.engine.input

import com.bwsw.sj.common.StreamConstants
import com.bwsw.sj.engine.core.{TStreamTaskInputService, PersistentBlockingQueue}
import com.bwsw.sj.engine.core.engine.input.TaskInputService
import com.bwsw.sj.engine.core.entities.Envelope
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
    envelope.streamType match {
      case StreamConstants.tStreamType =>
        tStreamRegularTaskInputService.registerEnvelope(envelope, performanceMetrics)
      case StreamConstants.kafkaStreamType =>
        kafkaRegularTaskInputService.registerEnvelope(envelope, performanceMetrics)
      case _ =>
        logger.error(s"Input stream type: ${envelope.streamType} is not defined for regular streaming engine")
        throw new Exception(s"Input stream type: ${envelope.streamType} is not defined for regular streaming engine")
    }
  }

  def call() = {
    tStreamRegularTaskInputService.call()
    kafkaRegularTaskInputService.call()
  }

  override def doCheckpoint() = kafkaRegularTaskInputService.doCheckpoint()
}
