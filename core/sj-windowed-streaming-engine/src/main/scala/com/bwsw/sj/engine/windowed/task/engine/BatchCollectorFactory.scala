package com.bwsw.sj.engine.windowed.task.engine

import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.entities.Batch
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.windowed.task.engine.collecting.{BatchCollector, NumericalBatchCollecting, TimeBatchCollecting, TransactionBatchCollecting}
import com.bwsw.sj.engine.windowed.task.reporting.WindowedStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

/**
 * Factory is in charge of creating of a task engine of windowed module
 *
 *
 * @param manager Manager of environment of task of windowed module
 * @param performanceMetrics Set of metrics that characterize performance of a windowed streaming module

 * @author Kseniya Mikhaleva
 */

class BatchCollectorFactory(manager: CommonTaskManager,
                            envelopeQueue: PersistentBlockingQueue,
                            batchQueue: ArrayBlockingQueue[Batch],
                            performanceMetrics: WindowedStreamingPerformanceMetrics) {

  protected val logger = LoggerFactory.getLogger(this.getClass)
  private val windowedInstance = manager.instance.asInstanceOf[WindowedInstance]

  def createBatchCollector(): BatchCollector = {
    windowedInstance.batchFillType.typeName match {
      case EngineLiterals.everyNthMode =>
        logger.info(s"Task: ${manager.taskName}. Windowed module has an '${EngineLiterals.everyNthMode}' batch fill type, create an appropriate batch collector\n")
        new BatchCollector(manager, envelopeQueue, batchQueue, performanceMetrics) with NumericalBatchCollecting
      case EngineLiterals.timeIntervalMode =>
        logger.info(s"Task: ${manager.taskName}. Windowed module has a '${EngineLiterals.timeIntervalMode}' batch fill type, create an appropriate batch collector\n")
        new BatchCollector(manager, envelopeQueue, batchQueue, performanceMetrics) with TimeBatchCollecting
      case EngineLiterals.transactionIntervalMode =>
        logger.info(s"Task: ${manager.taskName}. Windowed module has a '${EngineLiterals.transactionIntervalMode}' batch fill type, create an appropriate batch collector\n")
        new BatchCollector(manager, envelopeQueue, batchQueue, performanceMetrics) with TransactionBatchCollecting
    }
  }
}
