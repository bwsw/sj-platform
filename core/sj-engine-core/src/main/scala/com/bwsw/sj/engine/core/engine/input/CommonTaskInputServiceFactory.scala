package com.bwsw.sj.engine.core.engine.input

import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import org.slf4j.LoggerFactory

/**
 * Factory is in charge of creating of a task input service of regular engine
 *
 *
 * @author Kseniya Mikhaleva
 *
 * @param manager Manager of environment of task of regular module
 * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
 *                      which will be retrieved into a module
 */
class CommonTaskInputServiceFactory(manager: CommonTaskManager,
                                     blockingQueue: PersistentBlockingQueue) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val isKafkaInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.kafkaStreamType)
  private val isTstreamInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.tStreamType)

  def createTaskInputService() = {
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
