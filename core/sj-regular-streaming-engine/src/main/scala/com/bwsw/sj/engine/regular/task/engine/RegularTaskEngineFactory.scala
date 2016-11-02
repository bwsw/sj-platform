package com.bwsw.sj.engine.regular.task.engine

import com.bwsw.sj.common.DAL.model.module.RegularInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.{NumericalCheckpointTaskEngine, TimeCheckpointTaskEngine}
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

/**
 * Factory is in charge of creating of a task engine of regular module
 *
 *
 * @param manager Manager of environment of task of regular module
 * @param performanceMetrics Set of metrics that characterize performance of a regular streaming module

 * @author Kseniya Mikhaleva
 */

class RegularTaskEngineFactory(manager: CommonTaskManager,
                               performanceMetrics: RegularStreamingPerformanceMetrics) {

  protected val logger = LoggerFactory.getLogger(this.getClass)
  private val regularInstance = manager.instance.asInstanceOf[RegularInstance]

  /**
   * Creates RegularTaskEngine is in charge of a basic execution logic of task of regular module
   * @return Engine of regular task
   */
  def createRegularTaskEngine(): RegularTaskEngine = {
    regularInstance.checkpointMode match {
      case EngineLiterals.`timeIntervalMode` =>
        logger.info(s"Task: ${manager.taskName}. Regular module has a '${EngineLiterals.timeIntervalMode}' checkpoint mode, create an appropriate task engine\n")
        new RegularTaskEngine(manager, performanceMetrics) with TimeCheckpointTaskEngine
      case EngineLiterals.`everyNthMode` =>
        logger.info(s"Task: ${manager.taskName}. Regular module has an '${EngineLiterals.everyNthMode}' checkpoint mode, create an appropriate task engine\n")
        new RegularTaskEngine(manager, performanceMetrics) with NumericalCheckpointTaskEngine

    }
  }
}
