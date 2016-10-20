package com.bwsw.sj.engine.windowed.task.engine

import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.entities.Batch
import com.bwsw.sj.engine.windowed.task.WindowedTaskManager
import com.bwsw.sj.engine.windowed.task.engine.collecting.BatchCollector
import com.bwsw.sj.engine.windowed.task.reporting.WindowedStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

/**
 * Factory is in charge of creating of a task engine of windowed module
 *
 *
 * @param manager Manager of environment of task of windowed module
 * @param performanceMetrics Set of metrics that characterize performance of a windowed streaming module

 * @author Kseniya Mikhaleva
 */

class WindowedTaskEngineFactory(manager: WindowedTaskManager,
                               performanceMetrics: WindowedStreamingPerformanceMetrics,
                                batchQueue: ArrayBlockingQueue[Batch]) {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Creates WindowedTaskEngine is in charge of a basic execution logic of task of windowed module
   * @return Engine of windowed task
   */
  def createWindowedTaskEngine(): BatchCollector = {
    manager.windowedInstance.relatedStreams.nonEmpty match {
      case true =>
        logger.info(s"Task: ${manager.taskName}. Regular module has a '${EngineLiterals.timeIntervalMode}' checkpoint mode, create an appropriate task engine\n")
        new BatchCollector(manager, batchQueue, performanceMetrics)
      case false =>
        new BatchCollector(manager, batchQueue, performanceMetrics)

    }
  }
}
