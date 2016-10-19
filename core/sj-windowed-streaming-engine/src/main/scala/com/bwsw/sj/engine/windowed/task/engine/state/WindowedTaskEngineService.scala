package com.bwsw.sj.engine.windowed.task.engine.state

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.common.utils.SjTimer
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.engine.core.windowed.WindowedStreamingExecutor
import com.bwsw.sj.engine.windowed.task.WindowedTaskManager
import com.bwsw.sj.engine.windowed.task.reporting.WindowedStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

/**
 * Class is in charge of creating a specific ModuleEnvironmentManager (and executor)
 * depending on an instance parameter 'state-management' and performing the appropriate actions related with checkpoint
 *
 * @param manager Manager of environment of task of windowed module
 * @param performanceMetrics Set of metrics that characterize performance of a windowed streaming module
 */
abstract class WindowedTaskEngineService(manager: WindowedTaskManager, performanceMetrics: WindowedStreamingPerformanceMetrics) {

  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected val windowedInstance = manager.instance.asInstanceOf[WindowedInstance]
  protected val outputProducers = manager.outputProducers
  val moduleTimer = new SjTimer()
  val outputTags = manager.outputTags

  val windowedEnvironmentManager: ModuleEnvironmentManager
  val executor: WindowedStreamingExecutor

  def doCheckpoint(): Unit
}
