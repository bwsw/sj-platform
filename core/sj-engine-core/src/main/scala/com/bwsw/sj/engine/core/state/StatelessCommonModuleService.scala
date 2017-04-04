package com.bwsw.sj.engine.core.state

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup

/**
 * Class is in charge of creating a ModuleEnvironmentManager (and executor)
 *
 * @param manager Manager of environment of task of regular module
 * @param performanceMetrics Set of metrics that characterize performance of a regular streaming module
 */
class StatelessCommonModuleService(manager: CommonTaskManager, checkpointGroup: CheckpointGroup, performanceMetrics: PerformanceMetrics)
  extends CommonModuleService(manager, checkpointGroup, performanceMetrics) {
  private val streamService = ConnectionRepository.getStreamService

  val environmentManager = new ModuleEnvironmentManager(
    instance.getOptionsAsMap(),
    outputProducers,
    instance.outputs.flatMap(x => streamService.get(x)),
    producerPolicyByOutput,
    moduleTimer,
    performanceMetrics,
    manager.moduleClassLoader
  )

  val executor = manager.getExecutor(environmentManager)
}
