package com.bwsw.sj.engine.windowed.task.engine.input

import java.util.Date

import com.bwsw.sj.common.DAL.model.TStreamSjStream
import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{EngineLiterals, StreamLiterals}
import com.bwsw.sj.engine.core.entities.TStreamEnvelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.consumer.Offset.{DateTime, IOffset, Newest, Oldest}
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
 * Class is responsible for launching t-stream consumers
 * that allow to fetching messages, which are wrapped in envelope
 *
 * @author Kseniya Mikhaleva
 *
 */
class TStreamInput(manager: CommonTaskManager,
                   override val checkpointGroup: CheckpointGroup = new CheckpointGroup())
  extends Input[TStreamEnvelope](manager.inputs) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val instance = manager.instance.asInstanceOf[WindowedInstance]
  private val tstreamOffsetsStorage = mutable.Map[(String, Int), Long]()
  private val consumers = createConsumers()
  addConsumersToCheckpointGroup()
  launchConsumers()

  private def createConsumers() = {
    logger.debug(s"Task: ${manager.taskName}. Start creating consumers\n")
    val inputs = manager.inputs
    val offset = chooseOffset(instance.startFrom)

    val consumers = inputs.filter(x => x._1.streamType == StreamLiterals.tStreamType)
      .map(x => (x._1.asInstanceOf[TStreamSjStream], x._2.toList))
      .map(x => manager.createConsumer(x._1, x._2, offset))
      .map(x => (x.name, x)).toMap
    logger.debug(s"Task: ${manager.taskName}. Creation of consumers is finished\n")

    consumers
  }

  /**
   * Chooses offset policy for t-streams consumers
   *
   * @param startFrom Offset policy name or specific date
   */
  private def chooseOffset(startFrom: String): IOffset = {
    logger.debug(s"Choose offset policy for t-streams consumer\n")
    startFrom match {
      case EngineLiterals.oldestStartMode => Oldest
      case EngineLiterals.newestStartMode => Newest
      case time => DateTime(new Date(time.toLong * 1000))
    }
  }

  override def get() = {
    consumers.flatMap(x => {
      val consumer = x._2
      val stream = ConnectionRepository.getStreamService.get(consumer.stream.getName).get
      val txns = consumer.getPartitions().toSeq.flatMap(partition => {
        val fromOffset = if (tstreamOffsetsStorage.isDefinedAt((consumer.name, partition))) tstreamOffsetsStorage((consumer.name, partition))
        else consumer.getCurrentOffset(partition)
        val toOffset = if (consumer.getLastTransaction(partition).isDefined) consumer.getLastTransaction(partition).get.getTransactionID() else fromOffset
        consumer.getTransactionsFromTo(partition, fromOffset, toOffset)
      })
      txns.map(transaction => {
        val tempTransaction = consumer.buildTransactionObject(transaction.getPartition(), transaction.getTransactionID(), transaction.getCount()).get //todo fix it
        tstreamOffsetsStorage((x._2.name, tempTransaction.getPartition())) = tempTransaction.getTransactionID()
        val envelope = new TStreamEnvelope()
        envelope.stream = stream.name
        envelope.partition = tempTransaction.getPartition()
        envelope.id = tempTransaction.getTransactionID()
        envelope.consumerName = x._2.name
        envelope.data = tempTransaction.getAll()
        envelope.tags = stream.tags

        envelope
      })
    })
  }

  private def addConsumersToCheckpointGroup() = {
    logger.debug(s"Task: ${manager.taskName}. Start adding subscribing consumers to checkpoint group\n")
    consumers.foreach(x => checkpointGroup.add(x._2))
    logger.debug(s"Task: ${manager.taskName}. Adding subscribing consumers to checkpoint group is finished\n")
  }

  private def launchConsumers() = {
    logger.debug(s"Task: ${manager.taskName}. Launch subscribing consumers\n")
    consumers.foreach(_._2.start())
    logger.debug(s"Task: ${manager.taskName}. Subscribing consumers are launched\n")
  }

  override def setConsumerOffset(envelope: TStreamEnvelope) = {
    logger.debug(s"Task: ${manager.taskName}. " +
      s"Change local offset of consumer: ${envelope.consumerName} to txn: ${envelope.id}\n")
    consumers(envelope.consumerName).setStreamPartitionOffset(envelope.partition, envelope.id)
  }
}
