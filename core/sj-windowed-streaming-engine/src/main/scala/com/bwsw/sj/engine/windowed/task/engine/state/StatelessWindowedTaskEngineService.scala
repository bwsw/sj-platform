package com.bwsw.sj.engine.windowed.task.engine.state

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.engine.core.windowed.WindowedStreamingExecutor
import com.bwsw.sj.engine.windowed.task.WindowedTaskManager
import com.bwsw.sj.engine.windowed.task.reporting.WindowedStreamingPerformanceMetrics

/**
 * Class is in charge of creating a ModuleEnvironmentManager (and executor)
 *
 * @param manager Manager of environment of task of windowed module
 * @param performanceMetrics Set of metrics that characterize performance of a windowed streaming module
 */
class StatelessWindowedTaskEngineService(manager: WindowedTaskManager, performanceMetrics: WindowedStreamingPerformanceMetrics)
  extends WindowedTaskEngineService(manager, performanceMetrics) {
  private val streamService = ConnectionRepository.getStreamService

  val windowedEnvironmentManager = new ModuleEnvironmentManager(
    windowedInstance.getOptionsAsMap(),
    outputProducers,
    windowedInstance.outputs
      .flatMap(x => streamService.get(x)),
    outputTags,
    moduleTimer,
    performanceMetrics
  )

  val executor = manager.getExecutor(windowedEnvironmentManager).asInstanceOf[WindowedStreamingExecutor]

  override def doCheckpoint(): Unit = {}
}
