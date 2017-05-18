package com.bwsw.sj.engine.core.state

import com.bwsw.sj.common.dal.model.instance.{BatchInstanceDomain, InstanceDomain, RegularInstanceDomain}
import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.common.utils.{EngineLiterals, SjTimer}
import com.bwsw.sj.engine.core.environment.{ModuleEnvironmentManager, ModuleOutput}
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.Producer
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

/**
  * Class is in charge of creating a specific ModuleEnvironmentManager (and executor)
  * depending on an instance parameter 'state-management' and performing the appropriate actions related with checkpoint
  *
  * @param manager            Manager of environment of task of regular module
  * @param performanceMetrics Set of metrics that characterize performance of a regular streaming module
  */
abstract class CommonModuleService(manager: CommonTaskManager, checkpointGroup: CheckpointGroup, performanceMetrics: PerformanceMetrics) {

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected val instance: InstanceDomain = manager.instance
  protected val outputProducers: Map[String, Producer] = manager.outputProducers
  val moduleTimer: SjTimer = new SjTimer()
  protected val producerPolicyByOutput: mutable.Map[String, (String, ModuleOutput)] = mutable.Map[String, (String, ModuleOutput)]()

  addProducersToCheckpointGroup()

  protected val environmentManager: ModuleEnvironmentManager
  val executor: StreamingExecutor

  private def addProducersToCheckpointGroup(): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Start adding t-stream producers to checkpoint group.")
    outputProducers.foreach(x => checkpointGroup.add(x._2))
    logger.debug(s"Task: ${manager.taskName}. The t-stream producers are added to checkpoint group.")
  }

  def doCheckpoint(): Unit = {
    producerPolicyByOutput.clear()
  }

  def isCheckpointInitiated: Boolean = environmentManager.isCheckpointInitiated
}

object CommonModuleService {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(manager: CommonTaskManager,
            checkpointGroup: CheckpointGroup,
            performanceMetrics: PerformanceMetrics): CommonModuleService = {

    val stateManagement = manager.instance match {
      case regularInstance: RegularInstanceDomain =>
        regularInstance.stateManagement
      case batchInstance: BatchInstanceDomain =>
        batchInstance.stateManagement
      case _ =>
        logger.error("CommonModuleService can be used only for regular or batch engine.")
        throw new RuntimeException("CommonModuleService can be used only for regular or batch engine.")
    }

    stateManagement match {
      case EngineLiterals.noneStateMode =>
        logger.debug(s"Task: ${manager.taskName}. Start preparing of batch module without a state.")
        new StatelessCommonModuleService(manager, checkpointGroup, performanceMetrics)
      case EngineLiterals.ramStateMode =>
        logger.debug(s"Task: ${manager.taskName}. Start preparing of batch module with a state.")
        new StatefulCommonModuleService(manager, checkpointGroup, performanceMetrics)
    }
  }
}