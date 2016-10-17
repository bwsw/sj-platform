package com.bwsw.sj.engine.windowed.task.engine.state

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.environment.RegularEnvironmentManager
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.windowed.task.WindowedTaskManager
import com.bwsw.sj.engine.windowed.task.reporting.WindowedStreamingPerformanceMetrics

/**
 * Class is in charge of creating a ModuleEnvironmentManager (and executor)
 *
 * @param manager Manager of environment of task of regular module
 * @param performanceMetrics Set of metrics that characterize performance of a regular streaming module
 */
class StatelessWindowedTaskEngineService(manager: WindowedTaskManager, performanceMetrics: WindowedStreamingPerformanceMetrics)
  extends WindowedTaskEngineService(manager, performanceMetrics) {
  private val streamService = ConnectionRepository.getStreamService

  val regularEnvironmentManager = new RegularEnvironmentManager(
    regularInstance.getOptionsAsMap(),
    outputProducers,
    regularInstance.outputs
      .flatMap(x => streamService.get(x)),
    outputTags,
    moduleTimer,
    performanceMetrics
  )

  val executor = manager.getExecutor(regularEnvironmentManager).asInstanceOf[RegularStreamingExecutor]

  override def doCheckpoint(): Unit = {}
}
