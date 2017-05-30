package com.bwsw.sj.engine.core.state


import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup
import scaldi.Injectable.inject
import scaldi.Injector

/**
  * Class is in charge of creating [[ModuleEnvironmentManager]] (and executor [[StreamingExecutor]])
  *
  * @param manager            manager of environment of task of [[EngineLiterals.regularStreamingType]] or [[EngineLiterals.batchStreamingType]] module
  * @param performanceMetrics set of metrics that characterize performance of [[EngineLiterals.regularStreamingType]] or [[EngineLiterals.batchStreamingType]] module
  */
class StatelessCommonModuleService(manager: CommonTaskManager,
                                   checkpointGroup: CheckpointGroup,
                                   performanceMetrics: PerformanceMetrics)
                                  (implicit injector: Injector)
  extends CommonModuleService(manager, checkpointGroup, performanceMetrics) {
  private val streamService = inject[ConnectionRepository].getStreamRepository

  val environmentManager: ModuleEnvironmentManager = new ModuleEnvironmentManager(
    instance.options,
    outputProducers,
    instance.outputs.flatMap(x => streamService.get(x)),
    producerPolicyByOutput,
    moduleTimer,
    performanceMetrics,
    manager.moduleClassLoader
  )

  val executor: StreamingExecutor = manager.getExecutor(environmentManager)
}
