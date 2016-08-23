package com.bwsw.sj.engine.regular.task.engine.input

import com.bwsw.sj.common.StreamConstants
import com.bwsw.sj.engine.core.{TStreamTaskInputService, PersistentBlockingQueue}
import com.bwsw.sj.engine.regular.task.RegularTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory

/**
 * Factory is in charge of creating of a task input service of regular engine
 * Created: 27/07/2016
 *
 * @author Kseniya Mikhaleva
 *
 * @param manager Manager of environment of task of regular module
 * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
 *                      which will be retrieved into a module
 * @param checkpointGroup Group of t-stream agents that have to make a checkpoint at the same time
 */
class RegularTaskInputServiceFactory(manager: RegularTaskManager,
                                     blockingQueue: PersistentBlockingQueue,
                                     checkpointGroup: CheckpointGroup) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val isKafkaInputExist = manager.inputs.exists(x => x._1.streamType == StreamConstants.kafkaStreamType)
  private val isTstreamInputExist = manager.inputs.exists(x => x._1.streamType == StreamConstants.tStreamType)

  def createRegularTaskInputService() = {
    (isKafkaInputExist, isTstreamInputExist) match {
      case (true, true) => new CompleteTaskInputService(manager, blockingQueue, checkpointGroup)
      case (false, true) => new TStreamTaskInputService(manager, blockingQueue, checkpointGroup)
      case (true, false) => new KafkaTaskInputService(manager, blockingQueue, checkpointGroup)
      case _ =>
        logger.error("Type of input stream is not 'kafka' or 't-stream'")
        throw new Exception("Type of input stream is not 'kafka' or 't-stream'")
    }
  }
}
