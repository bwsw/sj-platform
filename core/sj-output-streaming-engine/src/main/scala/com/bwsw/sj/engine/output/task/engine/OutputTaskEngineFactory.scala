package com.bwsw.sj.engine.output.task.engine

import com.bwsw.sj.engine.core.PersistentBlockingQueue
import com.bwsw.sj.engine.core.engine.{NumericalCheckpointTaskEngine, TimeCheckpointTaskEngine}
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

/**
 * Factory is in charge of creating of a task engine of output module
 *
 *
 * @param manager Manager of environment of task of output module
 * @param performanceMetrics Set of metrics that characterize performance of a output streaming module
 * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
 *                      which will be retrieved into a module

 * @author Kseniya Mikhaleva
 */

class OutputTaskEngineFactory(manager: OutputTaskManager,
                               performanceMetrics: OutputStreamingPerformanceMetrics,
                               blockingQueue: PersistentBlockingQueue) {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Creates OutputTaskEngine is in charge of a basic execution logic of task of regular module
   * @return Engine of output task
   */
  def createOutputTaskEngine(): OutputTaskEngine = {
    manager.instance.checkpointMode match {
      case "time-interval" =>
        logger.info(s"Task: ${manager.taskName}. Regular module has a 'time-interval' checkpoint mode, create an appropriate task engine\n")
        new OutputTaskEngine(manager, performanceMetrics, blockingQueue) with TimeCheckpointTaskEngine
      case "every-nth" =>
        logger.info(s"Task: ${manager.taskName}. Regular module has an 'every-nth' checkpoint mode, create an appropriate task engine\n")
        new OutputTaskEngine(manager, performanceMetrics, blockingQueue) with NumericalCheckpointTaskEngine
    }
  }
}

