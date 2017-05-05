package com.bwsw.sj.engine.batch.task.input

import java.util.Date

import com.bwsw.sj.common.dal.model.module.BatchInstance
import com.bwsw.sj.common.dal.model.stream.TStreamSjStream
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.engine.EnvelopeDataSerializer
import com.bwsw.sj.common.utils.{EngineLiterals, StreamLiterals}
import com.bwsw.sj.engine.core.entities.TStreamEnvelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.consumer.Offset.{DateTime, IOffset, Newest, Oldest}
import com.bwsw.tstreams.agents.consumer.{Consumer, ConsumerTransaction}
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
class RetrievableTStreamCheckpointTaskInput[T <: AnyRef](manager: CommonTaskManager,
                                                         override val checkpointGroup: CheckpointGroup = new CheckpointGroup())
  extends RetrievableCheckpointTaskInput[TStreamEnvelope[T]](manager.inputs) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val instance = manager.instance.asInstanceOf[BatchInstance]
  private val tstreamOffsetsStorage = mutable.Map[(String, Int), Long]()
  private val envelopeDataSerializer = manager.envelopeDataSerializer.asInstanceOf[EnvelopeDataSerializer[T]]
  private val consumers = createConsumers()
  addConsumersToCheckpointGroup()
  launchConsumers()

  private def createConsumers() = {
    logger.debug(s"Task: ${manager.taskName}. Start creating consumers.")
    val inputs = manager.inputs
    val offset = chooseOffset(instance.startFrom)

    val consumers = inputs.filter(x => x._1.streamType == StreamLiterals.tstreamType)
      .map(x => (x._1.asInstanceOf[TStreamSjStream], x._2.toList))
      .map(x => manager.createConsumer(x._1, x._2, offset))
      .map(x => (x.name, x)).toMap
    logger.debug(s"Task: ${manager.taskName}. Creation of consumers is finished.")

    consumers
  }

  /**
    * Chooses offset policy for t-streams consumers
    *
    * @param startFrom Offset policy name or specific date
    */
  private def chooseOffset(startFrom: String): IOffset = {
    logger.debug(s"Choose offset policy for t-streams consumer.")
    startFrom match {
      case EngineLiterals.oldestStartMode => Oldest
      case EngineLiterals.newestStartMode => Newest
      case time => DateTime(new Date(time.toLong * 1000))
    }
  }

  override def get() = {
    consumers.flatMap(x => {
      val consumer = x._2
      val transactions = getAvailableTransactions(consumer)
      transactionsToEnvelopes(transactions, consumer)
    })
  }

  private def getAvailableTransactions(consumer: Consumer) = {
    consumer.getPartitions().toSeq.flatMap(partition => {
      val fromOffset = getFromOffset(consumer, partition)
      val lastTransaction = consumer.getLastTransaction(partition)
      val toOffset = if (lastTransaction.isDefined) lastTransaction.get.getTransactionID() else fromOffset
      consumer.getTransactionsFromTo(partition, fromOffset, toOffset)
    })
  }

  private def transactionsToEnvelopes(transactions: Seq[ConsumerTransaction], consumer: Consumer) = {
    val stream = ConnectionRepository.getStreamService.get(consumer.stream.name).get
    transactions.map((transaction: ConsumerTransaction) => {
      val tempTransaction = consumer.buildTransactionObject(transaction.getPartition(), transaction.getTransactionID(), transaction.getCount()).get //todo fix it next milestone TR1216
      tstreamOffsetsStorage((consumer.name, tempTransaction.getPartition())) = tempTransaction.getTransactionID()
      val data = transaction.getAll().map(envelopeDataSerializer.deserialize)
      val envelope = new TStreamEnvelope(data, consumer.name)
      envelope.stream = stream.name
      envelope.partition = tempTransaction.getPartition()
      envelope.tags = stream.tags
      envelope.id = tempTransaction.getTransactionID()

      envelope
    })
  }

  private def getFromOffset(consumer: Consumer, partition: Int) = {
    if (tstreamOffsetsStorage.isDefinedAt((consumer.name, partition))) tstreamOffsetsStorage((consumer.name, partition))
    else consumer.getCurrentOffset(partition)
  }

  private def addConsumersToCheckpointGroup() = {
    logger.debug(s"Task: ${manager.taskName}. Start adding subscribing consumers to checkpoint group.")
    consumers.foreach(x => checkpointGroup.add(x._2))
    logger.debug(s"Task: ${manager.taskName}. Adding subscribing consumers to checkpoint group is finished.")
  }

  private def launchConsumers() = {
    logger.debug(s"Task: ${manager.taskName}. Launch subscribing consumers.")
    consumers.foreach(_._2.start())
    logger.debug(s"Task: ${manager.taskName}. Subscribing consumers are launched.")
  }

  override def setConsumerOffset(envelope: TStreamEnvelope[T]) = {
    logger.debug(s"Task: ${manager.taskName}. " +
      s"Change local offset of consumer: ${envelope.consumerName} to txn: ${envelope.id}.")
    consumers(envelope.consumerName).setStreamPartitionOffset(envelope.partition, envelope.id)
  }
}
