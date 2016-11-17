package com.bwsw.sj.engine.windowed.task.engine.collecting

import java.util.concurrent.{ArrayBlockingQueue, Callable}

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.common.utils.{EngineLiterals, SjStreamUtils}
import com.bwsw.sj.engine.core.entities._
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.windowed.task.engine.input.TaskInput

import com.bwsw.sj.engine.windowed.task.reporting.WindowedStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

import scala.collection.Map

/**
 * Provides methods are responsible for a basic execution logic of task of windowed module
 *
 *
 * @param manager Manager of environment of task of windowed module
 * @param performanceMetrics Set of metrics that characterize performance of a windowed streaming module

 * @author Kseniya Mikhaleva
 */
abstract class BatchCollector(protected val manager: CommonTaskManager,
                              inputService: TaskInput[_ >: TStreamEnvelope with KafkaEnvelope <: Envelope],
                              batchQueue: ArrayBlockingQueue[Batch],
                              performanceMetrics: WindowedStreamingPerformanceMetrics) extends Callable[Unit] {

  import BatchCollector._

  private val currentThread = Thread.currentThread()
  currentThread.setName(s"windowed-task-${manager.taskName}-batch-collector")
  protected val instance = manager.instance.asInstanceOf[WindowedInstance]
  private val mainStream = SjStreamUtils.clearStreamFromMode(instance.mainStream)
  private val batchPerStream: Map[String, Batch] = createStorageOfBatches()

  private def createStorageOfBatches() = {
    manager.inputs.map(x => (x._1.name, new Batch(x._1.name, x._1.tags)))
  }

  override def call(): Unit = {
    while (true) {
      inputService.get().foreach(envelope => {
        registerEnvelope(envelope)

        if (mainStream == envelope.stream) {
          afterReceivingEnvelope(envelope)
        }

        if (isItTimeToCollectBatch()) collectBatch()
      })
    }
  }

  private def registerEnvelope(envelope: Envelope) = {
    batchPerStream(envelope.stream).envelopes += envelope
    performanceMetrics.addEnvelopeToInputStream(envelope)
  }

  protected def collectBatch() = {
    logger.info(s"Task: ${manager.taskName}. It's time to collect batch\n")
    val (mainStreamBatches, relatedStreamBatches) = batchPerStream.partition(x => x._1 == mainStream)
    putBatchesIntoQueue(relatedStreamBatches.values)
    putBatchesIntoQueue(mainStreamBatches.values)
    clearBatches()
    prepareForNextBatchCollecting()
  }

  private def putBatchesIntoQueue(batchPerStream: Iterable[Batch]) = {
    batchPerStream.foreach(x => batchQueue.put(x.copy()))
  }

  private def clearBatches() = {
    batchPerStream.foreach(x => x._2.envelopes.clear())
  }

  protected def afterReceivingEnvelope(envelope: Envelope)

  protected def isItTimeToCollectBatch(): Boolean

  protected def prepareForNextBatchCollecting()
}

object BatchCollector {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(manager: CommonTaskManager,
            inputService: TaskInput[_ >: TStreamEnvelope with KafkaEnvelope <: Envelope],
            batchQueue: ArrayBlockingQueue[Batch],
            performanceMetrics: WindowedStreamingPerformanceMetrics) = {
    val windowedInstance = manager.instance.asInstanceOf[WindowedInstance]

    windowedInstance.batchFillType.typeName match {
      case EngineLiterals.everyNthMode =>
        logger.info(s"Task: ${manager.taskName}. Windowed module has an '${EngineLiterals.everyNthMode}' batch fill type, create an appropriate batch collector\n")
        new BatchCollector(manager, inputService, batchQueue, performanceMetrics) with NumericalBatchCollecting
      case EngineLiterals.timeIntervalMode =>
        logger.info(s"Task: ${manager.taskName}. Windowed module has a '${EngineLiterals.timeIntervalMode}' batch fill type, create an appropriate batch collector\n")
        new BatchCollector(manager, inputService, batchQueue, performanceMetrics) with TimeBatchCollecting
      case EngineLiterals.transactionIntervalMode =>
        logger.info(s"Task: ${manager.taskName}. Windowed module has a '${EngineLiterals.transactionIntervalMode}' batch fill type, create an appropriate batch collector\n")
        new BatchCollector(manager, inputService, batchQueue, performanceMetrics) with TransactionBatchCollecting
    }
  }
}





