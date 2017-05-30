package com.bwsw.sj.engine.batch.task.input

import java.util.Date

import com.bwsw.sj.common.dal.model.stream.{StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.engine.EnvelopeDataSerializer
import com.bwsw.sj.common.si.model.instance.BatchInstance
import com.bwsw.sj.common.utils.{EngineLiterals, StreamLiterals}
import com.bwsw.sj.engine.core.entities.TStreamEnvelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.consumer.Offset.{DateTime, IOffset, Newest, Oldest}
import com.bwsw.tstreams.agents.consumer.{Consumer, ConsumerTransaction}
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreamstransactionserver.rpc.TransactionStates
import org.slf4j.LoggerFactory
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.immutable.Iterable
import scala.collection.mutable

/**
  * Class is responsible for handling t-stream input
  * (i.e. retrieving and checkpointing kafka and t-stream messages)
  * for batch streaming engine.
  * It launches t-stream consumers allowing to fetch messages, which are wrapped in envelopes,
  * and checkpoint processed messages
  *
  * @param manager         allows to manage an environment of batch streaming task
  * @param checkpointGroup group of t-stream agents that have to make a checkpoint at the same time
  * @author Kseniya Mikhaleva
  */
class RetrievableTStreamCheckpointTaskInput[T <: AnyRef](manager: CommonTaskManager,
                                                         override val checkpointGroup: CheckpointGroup = new CheckpointGroup())
                                                        (implicit injector: Injector)
  extends RetrievableCheckpointTaskInput[TStreamEnvelope[T]](manager.inputs) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val instance = manager.instance.asInstanceOf[BatchInstance]
  private val tstreamOffsetsStorage = mutable.Map[(String, Int), Long]()
  private val envelopeDataSerializer = manager.envelopeDataSerializer.asInstanceOf[EnvelopeDataSerializer[T]]
  private val consumers = createConsumers()
  addConsumersToCheckpointGroup()
  launchConsumers()

  private def createConsumers(): Map[String, Consumer] = {
    logger.debug(s"Task: ${manager.taskName}. Start creating consumers.")
    val inputs = manager.inputs
    val offset = chooseOffset(instance.startFrom)

    val consumers = inputs.filter(x => x._1.streamType == StreamLiterals.tstreamType)
      .map(x => (x._1.asInstanceOf[TStreamStreamDomain], x._2.toList))
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

  override def get(): Iterable[TStreamEnvelope[T]] = {
    consumers.flatMap(x => {
      val consumer = x._2
      val transactions = getAvailableTransactions(consumer)
      transactionsToEnvelopes(transactions, consumer)
    })
  }

  private def getAvailableTransactions(consumer: Consumer): Seq[ConsumerTransaction] = {
    consumer.getPartitions.toSeq.flatMap(partition => {
      val fromOffset = getFromOffset(consumer, partition)
      val lastTransaction = consumer.getLastTransaction(partition)
      val toOffset = if (lastTransaction.isDefined) lastTransaction.get.getTransactionID else fromOffset
      consumer.getTransactionsFromTo(partition, fromOffset, toOffset)
    })
  }

  private def transactionsToEnvelopes(transactions: Seq[ConsumerTransaction], consumer: Consumer): Seq[TStreamEnvelope[T]] = {
    val stream: StreamDomain = inject[ConnectionRepository].getStreamRepository.get(consumer.stream.name).get
    transactions.map(transaction => buildTransactionObject(transaction, consumer))
      .filter(_.getState != TransactionStates.Invalid)
      .map(transaction => createEnvelope(transaction, consumer.name, stream))
  }

  private def buildTransactionObject(transaction: ConsumerTransaction, consumer: Consumer): ConsumerTransaction = {
    val tempTransaction = consumer.buildTransactionObject(transaction.getPartition, transaction.getTransactionID, transaction.getState, transaction.getCount).get //todo fix it next milestone TR1216
    tstreamOffsetsStorage((consumer.name, tempTransaction.getPartition)) = tempTransaction.getTransactionID

    tempTransaction
  }

  private def createEnvelope(transaction: ConsumerTransaction, consumerName: String, stream: StreamDomain) = {
    val data = transaction.getAll.map(envelopeDataSerializer.deserialize)
    val envelope = new TStreamEnvelope(data, consumerName)
    envelope.stream = stream.name
    envelope.partition = transaction.getPartition
    envelope.tags = stream.tags
    envelope.id = transaction.getTransactionID

    envelope
  }

  private def getFromOffset(consumer: Consumer, partition: Int): Long = {
    if (tstreamOffsetsStorage.isDefinedAt((consumer.name, partition))) tstreamOffsetsStorage((consumer.name, partition))
    else consumer.getCurrentOffset(partition)
  }

  private def addConsumersToCheckpointGroup(): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Start adding subscribing consumers to checkpoint group.")
    consumers.foreach(x => checkpointGroup.add(x._2))
    logger.debug(s"Task: ${manager.taskName}. Adding subscribing consumers to checkpoint group is finished.")
  }

  private def launchConsumers(): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Launch subscribing consumers.")
    consumers.foreach(_._2.start())
    logger.debug(s"Task: ${manager.taskName}. Subscribing consumers are launched.")
  }

  override def setConsumerOffset(envelope: TStreamEnvelope[T]): Unit = {
    logger.debug(s"Task: ${manager.taskName}. " +
      s"Change local offset of consumer: ${envelope.consumerName} to txn: ${envelope.id}.")
    consumers(envelope.consumerName).setStreamPartitionOffset(envelope.partition, envelope.id)
  }

  override def close(): Unit = {
    consumers.foreach(_._2.stop())
  }
}
