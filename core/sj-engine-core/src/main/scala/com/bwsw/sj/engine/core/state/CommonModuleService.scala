package com.bwsw.sj.engine.core.state

import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.common.utils.SjTimer
import com.bwsw.sj.engine.core.environment.{ModuleEnvironmentManager, ModuleOutput}
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
 * Class is in charge of creating a specific ModuleEnvironmentManager (and executor)
 * depending on an instance parameter 'state-management' and performing the appropriate actions related with checkpoint
 *
 * @param manager Manager of environment of task of regular module
 * @param performanceMetrics Set of metrics that characterize performance of a regular streaming module
 */
abstract class CommonModuleService(manager: CommonTaskManager, checkpointGroup: CheckpointGroup, performanceMetrics: PerformanceMetrics) {

  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected val instance = manager.instance
  protected val outputProducers = manager.outputProducers
  val moduleTimer = new SjTimer()
  protected val producerPolicyByOutput = mutable.Map[String, (String, ModuleOutput)]()

  addProducersToCheckpointGroup()

  protected val environmentManager: ModuleEnvironmentManager
  val executor: StreamingExecutor

  private def addProducersToCheckpointGroup() = {
    logger.debug(s"Task: ${manager.taskName}. Start adding t-stream producers to checkpoint group\n")
    outputProducers.foreach(x => checkpointGroup.add(x._2))
    logger.debug(s"Task: ${manager.taskName}. The t-stream producers are added to checkpoint group\n")
  }

  def doCheckpoint(): Unit = {
    producerPolicyByOutput.clear()
  }

  def isCheckpointInitiated = environmentManager.isCheckpointInitiated
}