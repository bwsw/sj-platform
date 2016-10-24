package com.bwsw.sj.engine.core.state

import com.bwsw.sj.common.DAL.model.module.{WindowedInstance, RegularInstance}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.environment.StatefulModuleEnvironmentManager
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup

/**
 * Class is in charge of creating a StatefulModuleEnvironmentManager (and executor)
 * and performing a saving of state
 *
 * @param manager Manager of environment of task of regular module
 * @param checkpointGroup Group of t-stream agents that have to make a checkpoint at the same time
 * @param performanceMetrics Set of metrics that characterize performance of a regular streaming module
 */
class StatefulCommonModuleService(manager: CommonTaskManager, checkpointGroup: CheckpointGroup, performanceMetrics: PerformanceMetrics)
  extends CommonModuleService(manager, checkpointGroup, performanceMetrics) {

  private val streamService = ConnectionRepository.getStreamService
  private var countOfCheckpoints = 1
  private val stateService = new RAMStateService(manager, checkpointGroup)

  val environmentManager = new StatefulModuleEnvironmentManager(
    new StateStorage(stateService),
    instance.getOptionsAsMap(),
    outputProducers,
    instance.outputs
      .flatMap(x => streamService.get(x)),
    producerPolicyByOutput,
    moduleTimer,
    performanceMetrics
  )

  val executor = manager.getExecutor(environmentManager).asInstanceOf[RegularStreamingExecutor] //todo подумать над тем, как преобразовать к regular/window одновременно

  /**
   * Does group checkpoint of t-streams state consumers/producers
   */
  override def doCheckpoint() = {
    if (countOfCheckpoints != getStateFullCheckpoint()) {
      doCheckpointOfPartOfState()
    } else {
      doCheckpointOfFullState()
    }
    super.doCheckpoint()
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

  private def getStateFullCheckpoint() = {
    instance match {
      case regularInstance: RegularInstance =>  regularInstance.stateFullCheckpoint
      case windowedInstance: WindowedInstance =>  windowedInstance.stateFullCheckpoint
    }
  }
}