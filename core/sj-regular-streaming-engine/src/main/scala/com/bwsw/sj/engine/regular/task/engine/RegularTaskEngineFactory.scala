package com.bwsw.sj.engine.regular.task.engine

import com.bwsw.sj.engine.core.PersistentBlockingQueue
import com.bwsw.sj.engine.regular.task.RegularTaskManager
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

/**
 * Factory is in charge of creating of a task engine of regular module
 * Created: 27/07/2016
 *
 * @param manager Manager of environment of task of regular module
 * @param performanceMetrics Set of metrics that characterize performance of a regular streaming module
 * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
 *                      which will be retrieved into a module

 * @author Kseniya Mikhaleva
 */

class RegularTaskEngineFactory(manager: RegularTaskManager,
                               performanceMetrics: RegularStreamingPerformanceMetrics,
                               blockingQueue: PersistentBlockingQueue) {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Input instance is a metadata for running a task of regular module
   */
  private val inputInstanceMetadata = manager.getInstance

  /**
   * Creates RegularTaskEngine is in charge of a basic execution logic of task of regular module
   * @return Engine of input task
   */
  def createRegularTaskEngine(): RegularTaskEngine = {
    inputInstanceMetadata.checkpointMode match {
      case "time-interval" =>
        logger.info(s"Task: ${manager.taskName}. Regular module has a 'time-interval' checkpoint mode, create an appropriate task engine\n")
        logger.debug(s"Task: ${manager.taskName}. Create TimeCheckpointInputTaskEngine()\n")
        new TimeCheckpointRegularTaskEngine(manager, performanceMetrics, blockingQueue)
      case "every-nth" =>
        logger.info(s"Task: ${manager.taskName}. Regular module has an 'every-nth' checkpoint mode, create an appropriate task engine\n")
        new NumericalCheckpointRegularTaskEngine(manager, performanceMetrics, blockingQueue)

    }
  }
}
