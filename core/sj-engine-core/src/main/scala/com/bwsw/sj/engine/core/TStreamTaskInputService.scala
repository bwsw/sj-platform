package com.bwsw.sj.engine.core

import java.util.Date

import com.bwsw.sj.common.DAL.model.TStreamSjStream
import com.bwsw.sj.common.DAL.model.module.RegularInstance
import com.bwsw.sj.common.StreamConstants
import com.bwsw.sj.engine.core.engine.input.TaskInputService
import com.bwsw.sj.engine.core.entities.{Envelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.managment.TaskManager
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.consumer.Offset.{DateTime, IOffset, Newest, Oldest}
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory

/**
 * Class is responsible for launching t-stream subscribing consumers
 * that put consumed message, which are wrapped in envelope, into a common queue,
 * and processing the envelopes
 *
 *
 *
 * @param manager Manager of environment of task of regular module
 * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
 *                      which will be retrieved into a module
 * @param checkpointGroup Group of t-stream agents that have to make a checkpoint at the same time
 * @author Kseniya Mikhaleva
 *
 */
class TStreamTaskInputService(manager: TaskManager,
                              blockingQueue: PersistentBlockingQueue,
                              checkpointGroup: CheckpointGroup)
  extends TaskInputService {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val regularInstance = manager.instance.asInstanceOf[RegularInstance]
  private val consumers = createSubscribingConsumers()

  /**
   * Chooses offset policy for t-streams consumers
   *
   * @param startFrom Offset policy name or specific date
   * @return Offset
   */
  private def chooseOffset(startFrom: String): IOffset = {
    logger.debug(s"Choose offset policy for t-streams consumer\n")
    startFrom match {
      case "oldest" => Oldest
      case "newest" => Newest
      case time => DateTime(new Date(time.toLong * 1000))
    }
  }

  private def createSubscribingConsumers() = {
    logger.debug(s"Task: ${manager.taskName}. Start creating subscribing consumers\n")
    val inputs = manager.inputs
    val offset = regularInstance.startFrom
    val callback = new ConsumerCallback(blockingQueue)

    val consumers = inputs.filter(x => x._1.streamType == StreamConstants.tStreamType)
      .map(x => (x._1.asInstanceOf[TStreamSjStream], x._2.toList))
      .map(x => manager.createSubscribingConsumer(x._1, x._2, chooseOffset(offset), callback))
      .map(x => (x.name, x)).toMap
    logger.debug(s"Task: ${manager.taskName}. Creation of subscribing consumers is finished\n")

    consumers
  }

  private def launchConsumers() = {
    logger.debug(s"Task: ${manager.taskName}. Launch subscribing consumers\n")
    consumers.foreach(_._2.start())
    logger.debug(s"Task: ${manager.taskName}. Subscribing consumers are launched\n")
  }

  private def addConsumersToCheckpointGroup() = {
    logger.debug(s"Task: ${manager.taskName}. Start adding subscribing consumers to checkpoint group\n")
    consumers.foreach(x => checkpointGroup.add(x._2.getConsumer()))
    logger.debug(s"Task: ${manager.taskName}. Adding subscribing consumers to checkpoint group is finished\n")
  }

  def registerEnvelope(envelope: Envelope, performanceMetrics: PerformanceMetrics) = {
    logger.info(s"Task: ${manager.taskName}. T-stream envelope is received\n")
    val tStreamEnvelope = envelope.asInstanceOf[TStreamEnvelope]
    logger.debug(s"Task: ${manager.taskName}. " +
      s"Change local offset of consumer: ${tStreamEnvelope.consumerName} to txn: ${tStreamEnvelope.txnUUID}\n")
    consumers(tStreamEnvelope.consumerName).getConsumer().setStreamPartitionOffset(tStreamEnvelope.partition, tStreamEnvelope.txnUUID)
    performanceMetrics.addEnvelopeToInputStream(
      tStreamEnvelope.stream,
      tStreamEnvelope.data.map(_.length)
    )
  }

  def call() = {
    addConsumersToCheckpointGroup()
    launchConsumers()
  }

  override def doCheckpoint(): Unit = {}
}
