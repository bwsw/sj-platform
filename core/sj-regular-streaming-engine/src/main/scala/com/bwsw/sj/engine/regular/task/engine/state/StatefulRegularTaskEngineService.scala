package com.bwsw.sj.engine.regular.task.engine.state

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.environment.StatefulModuleEnvironmentManager
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.core.state.StateStorage
import com.bwsw.sj.engine.regular.state.RAMStateService
import com.bwsw.sj.engine.regular.task.RegularTaskManager
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup

/**
 * Class is in charge of creating a StatefulModuleEnvironmentManager (and executor)
 * and performing a saving of state
 *
 * @param manager Manager of environment of task of regular module
 * @param checkpointGroup Group of t-stream agents that have to make a checkpoint at the same time
 * @param performanceMetrics Set of metrics that characterize performance of a regular streaming module
 */
class StatefulRegularTaskEngineService(manager: RegularTaskManager, checkpointGroup: CheckpointGroup, performanceMetrics: RegularStreamingPerformanceMetrics)
  extends RegularTaskEngineService(manager, performanceMetrics) {

  private var countOfCheckpoints = 1

  private val stateService = new RAMStateService(manager, checkpointGroup)

  val moduleEnvironmentManager = new StatefulModuleEnvironmentManager(
    new StateStorage(stateService),
    optionsSerializer.deserialize[Map[String, Any]](regularInstance.options),
    outputProducers,
    regularInstance.outputs
      .map(ConnectionRepository.getStreamService.get)
      .filter(_.tags != null),
    outputTags,
    moduleTimer,
    performanceMetrics
  )

  val executor = manager.getExecutor(moduleEnvironmentManager).asInstanceOf[RegularStreamingExecutor]

  /**
   * Does group checkpoint of t-streams state consumers/producers
   */
  override def doCheckpoint() = {
    if (countOfCheckpoints != regularInstance.stateFullCheckpoint) {
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
