package com.bwsw.sj.engine.windowed.task.engine.collecting

import java.util.concurrent.{ArrayBlockingQueue, Callable}

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.entities._
import com.bwsw.sj.engine.windowed.task.WindowedTaskManager
import com.bwsw.sj.engine.windowed.task.engine.input.WindowedTaskInputServiceFactory
import com.bwsw.sj.engine.windowed.task.reporting.WindowedStreamingPerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup
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
abstract class BatchCollector(protected val manager: WindowedTaskManager,
                     batchQueue: ArrayBlockingQueue[Batch],
                     performanceMetrics: WindowedStreamingPerformanceMetrics) extends Callable[Unit] {

  private val currentThread = Thread.currentThread()
  currentThread.setName(s"windowed-task-${manager.taskName}-batch-collector")
  protected val logger = LoggerFactory.getLogger(this.getClass)
  private val blockingQueue: PersistentBlockingQueue = new PersistentBlockingQueue(EngineLiterals.persistentBlockingQueue)
  protected val checkpointGroup = new CheckpointGroup()
  protected val instance = manager.instance.asInstanceOf[WindowedInstance]
  private val windowedTaskInputServiceFactory = new WindowedTaskInputServiceFactory(manager, blockingQueue, checkpointGroup)
  val taskInputService = windowedTaskInputServiceFactory.createRegularTaskInputService()

  private val envelopeSerializer = new JsonSerializer(true)
  private val batchPerStream: Map[String, Batch] = createStorageOfBatches()

  private def createStorageOfBatches() = {
    manager.inputs.map(x => (x._1.name, new Batch(x._1.name, x._1.tags)))
  }

  override def call(): Unit = {
    val lastEnvelopesByStreams = createStorageOfLastEnvelopes()

    while (true) {
      val maybeEnvelope = blockingQueue.get(instance.eventWaitTime)

      maybeEnvelope match {
        case Some(serializedEnvelope) => {
          val envelope = envelopeSerializer.deserialize[Envelope](serializedEnvelope)
          lastEnvelopesByStreams((envelope.stream, envelope.partition)) = envelope

          val transaction = envelope match {
            case kafkaEnvelope: KafkaEnvelope => Transaction(kafkaEnvelope.partition, kafkaEnvelope.offset, List(kafkaEnvelope.data))
            case tstreamEnvelope: TStreamEnvelope => Transaction(tstreamEnvelope.partition, tstreamEnvelope.id, tstreamEnvelope.data)
          }
          println("txn: " + envelope.stream + ", id: " + transaction.id)
          batchPerStream(envelope.stream).transactions += transaction

          if (instance.mainStream == envelope.stream) afterReceivingTransaction(transaction)
        }
        case None =>
      }

      if (isItTimeToCollectBatch()) collectBatch()
    }
  }

  private def createStorageOfLastEnvelopes() = {
    manager.inputs.flatMap(x => x._2.map(y => ((x._1.name, y), new Envelope())))
  }

  protected def collectBatch() = {
    logger.info(s"Task: ${manager.taskName}. It's time to collect batch\n")
    batchPerStream.foreach(x => {
      batchQueue.put(x._2.copy())
      println("put batch: " + x._1)
    })
    //todo либо класть в очередь батчей сначала все батчи для завивимых потоков, потом для основного
    //todo либо кол-чо батчей не будет совпадать у главного окна и зависимых окон
    batchPerStream.foreach(x => x._2.transactions.clear())
    prepareForNextBatchCollecting()
  }

  protected def afterReceivingTransaction(transaction: Transaction)

  protected def isItTimeToCollectBatch(): Boolean

  protected def prepareForNextBatchCollecting()
}









