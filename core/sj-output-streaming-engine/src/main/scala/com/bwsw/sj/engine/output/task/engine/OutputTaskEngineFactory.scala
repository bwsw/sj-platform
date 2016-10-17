package com.bwsw.sj.engine.output.task.engine

import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.NumericalCheckpointTaskEngine
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

/**
 * Factory is in charge of creating of a task engine of output module
 *
 *
 * @param manager Manager of environment of task of output module
 * @param performanceMetrics Set of metrics that characterize performance of a output streaming module

 * @author Kseniya Mikhaleva
 */

class OutputTaskEngineFactory(manager: OutputTaskManager,
                               performanceMetrics: OutputStreamingPerformanceMetrics) {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Creates OutputTaskEngine is in charge of a basic execution logic of task of output module
   * @return Engine of output task
   */
  def createOutputTaskEngine(): OutputTaskEngine = {
    manager.outputInstance.checkpointMode match {
      case EngineLiterals.`timeIntervalMode` =>
        logger.error(s"Task: ${manager.taskName}. Output module can't have a '${EngineLiterals.timeIntervalMode}' checkpoint mode\n")
        throw new Exception(s"Task: ${manager.taskName}. Output module can't have a '${EngineLiterals.timeIntervalMode}' checkpoint mode\n")
      case EngineLiterals.`everyNthMode` =>
        logger.info(s"Task: ${manager.taskName}. Output module has an '${EngineLiterals.everyNthMode}' checkpoint mode, create an appropriate task engine\n")
        new OutputTaskEngine(manager, performanceMetrics) with NumericalCheckpointTaskEngine
    }
  }
}

