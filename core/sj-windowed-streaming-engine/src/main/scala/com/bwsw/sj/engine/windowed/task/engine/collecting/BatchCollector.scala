package com.bwsw.sj.engine.windowed.task.engine.collecting

import java.util.concurrent.{ArrayBlockingQueue, Callable}

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.entities._
import com.bwsw.sj.engine.core.managment.CommonTaskManager
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
                              envelopeQueue: PersistentBlockingQueue,
                              batchQueue: ArrayBlockingQueue[Batch],
                              performanceMetrics: WindowedStreamingPerformanceMetrics) extends Callable[Unit] {

  private val currentThread = Thread.currentThread()
  currentThread.setName(s"windowed-task-${manager.taskName}-batch-collector")
  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected val instance = manager.instance.asInstanceOf[WindowedInstance]
  private val envelopeSerializer = new JsonSerializer(true)
  private val batchPerStream: Map[String, Batch] = createStorageOfBatches()

  private def createStorageOfBatches() = {
    manager.inputs.map(x => (x._1.name, new Batch(x._1.name, x._1.tags)))
  }

  override def call(): Unit = {
    while (true) {
      val maybeEnvelope = envelopeQueue.get(instance.eventWaitTime)

      maybeEnvelope match {
        case Some(serializedEnvelope) => {
          val envelope = envelopeSerializer.deserialize[Envelope](serializedEnvelope)
          println("envelope: " + envelope.asInstanceOf[TStreamEnvelope].id)
          batchPerStream(envelope.stream).envelopes += envelope

          if (instance.mainStream == envelope.stream) afterReceivingEnvelope(envelope)
        }
        case None =>
      }

      if (isItTimeToCollectBatch()) collectBatch()
    }
  }

  protected def collectBatch() = {
    logger.info(s"Task: ${manager.taskName}. It's time to collect batch\n")
    val (mainStreamBatches, relatedStreamBatches) = batchPerStream.partition(x => x._1 == instance.mainStream)
    putBatchesIntoQueue(relatedStreamBatches)
    putBatchesIntoQueue(mainStreamBatches)
    clearBatches()
    prepareForNextBatchCollecting()
  }

  private def putBatchesIntoQueue(batchPerStream: Map[String, Batch]) = {
    batchPerStream.foreach(x => batchQueue.put(x._2.copy()))
  }

  private def clearBatches() = {
    batchPerStream.foreach(x => x._2.envelopes.clear())
  }

  protected def afterReceivingEnvelope(envelope: Envelope)

  protected def isItTimeToCollectBatch(): Boolean

  protected def prepareForNextBatchCollecting()
}









