package com.bwsw.sj.engine.core.state

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.engine.StateHandlers
import com.bwsw.sj.common.si.model.instance.{BatchInstance, RegularInstance}
import com.bwsw.sj.engine.core.environment.StatefulModuleEnvironmentManager
import com.bwsw.sj.engine.core.managment.CommonTaskManager
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

  private val streamService = ConnectionRepository.getStreamRepository
  private var countOfCheckpoints = 1
  private val stateService = new RAMStateService(manager, checkpointGroup)

  val environmentManager = new StatefulModuleEnvironmentManager(
    new StateStorage(stateService),
    instance.options,
    outputProducers,
    instance.outputs.flatMap(x => streamService.get(x)),
    producerPolicyByOutput,
    moduleTimer,
    performanceMetrics,
    manager.moduleClassLoader
  )

  val executor = manager.getExecutor(environmentManager)

  private val statefulExecutor = executor.asInstanceOf[StateHandlers]
  /**
   * Does group checkpoint of t-streams state consumers/producers
   */
  override def doCheckpoint(): Unit = {
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
  private def doCheckpointOfPartOfState(): Unit = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint of a part of state.")
    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeStateSave() handler.")
    statefulExecutor.onBeforeStateSave(false)
    stateService.savePartialState()
    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterStateSave() handler.")
    statefulExecutor.onAfterStateSave(false)
    countOfCheckpoints += 1
  }

   /**
   * Saves a state
   */
  private def doCheckpointOfFullState(): Unit = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint of full state.")
    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeStateSave() handler.")
     statefulExecutor.onBeforeStateSave(true)
    stateService.saveFullState()
    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterStateSave() handler.")
     statefulExecutor.onAfterStateSave(true)
    countOfCheckpoints = 1
  }

  private def getStateFullCheckpoint(): Int = {
    instance match {
      case regularInstance: RegularInstance =>  regularInstance.stateFullCheckpoint
      case batchInstance: BatchInstance =>  batchInstance.stateFullCheckpoint
    }
  }
}