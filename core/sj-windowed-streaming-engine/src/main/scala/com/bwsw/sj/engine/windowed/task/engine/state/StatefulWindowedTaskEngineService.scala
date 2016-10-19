package com.bwsw.sj.engine.windowed.task.engine.state

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.environment.StatefulModuleEnvironmentManager
import com.bwsw.sj.engine.core.windowed.WindowedStreamingExecutor
import com.bwsw.sj.engine.core.state.StateStorage
import com.bwsw.sj.engine.windowed.state.RAMStateService
import com.bwsw.sj.engine.windowed.task.WindowedTaskManager
import com.bwsw.sj.engine.windowed.task.reporting.WindowedStreamingPerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup

/**
 * Class is in charge of creating a StatefulModuleEnvironmentManager (and executor)
 * and performing a saving of state
 *
 * @param manager Manager of environment of task of windowed module
 * @param checkpointGroup Group of t-stream agents that have to make a checkpoint at the same time
 * @param performanceMetrics Set of metrics that characterize performance of a windowed streaming module
 */
class StatefulWindowedTaskEngineService(manager: WindowedTaskManager, checkpointGroup: CheckpointGroup, performanceMetrics: WindowedStreamingPerformanceMetrics)
  extends WindowedTaskEngineService(manager, performanceMetrics) {

  private val streamService = ConnectionRepository.getStreamService
  private var countOfCheckpoints = 1
  private val stateService = new RAMStateService(manager, checkpointGroup)

  val windowedEnvironmentManager = new StatefulModuleEnvironmentManager(
    new StateStorage(stateService),
    windowedInstance.getOptionsAsMap(),
    outputProducers,
    windowedInstance.outputs
      .flatMap(x => streamService.get(x)),
    outputTags,
    moduleTimer,
    performanceMetrics
  )

  val executor = manager.getExecutor(windowedEnvironmentManager).asInstanceOf[WindowedStreamingExecutor]

  /**
   * Does group checkpoint of t-streams state consumers/producers
   */
  override def doCheckpoint() = {
    if (countOfCheckpoints != windowedInstance.stateFullCheckpoint) {
      doCheckpointOfPartOfState()
    } else {
      doCheckpointOfFullState()
    }
  }

  /**
   * Saves a partial state changes
   */
  private def doCheckpointOfPartOfState() = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint of a part of state\n")
    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeStateSave() handler\n")
    executor.onBeforeStateSave(false)
    stateService.savePartialState()
    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterStateSave() handler\n")
    executor.onAfterStateSave(false)
    countOfCheckpoints += 1
  }

   /**
   * Saves a state
   */
  private def doCheckpointOfFullState() = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint of full state\n")
    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeStateSave() handler\n")
    executor.onBeforeStateSave(true)
    stateService.saveFullState()
    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterStateSave() handler\n")
    executor.onAfterStateSave(true)
    countOfCheckpoints = 1
  }
}
