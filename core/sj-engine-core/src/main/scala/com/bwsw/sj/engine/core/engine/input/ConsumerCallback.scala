package com.bwsw.sj.engine.core.engine.input

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.entities.TStreamEnvelope
import com.bwsw.tstreams.agents.consumer.subscriber.Callback
import com.bwsw.tstreams.agents.consumer.{ConsumerTransaction, Consumer, TransactionOperator}
import org.slf4j.LoggerFactory


/**
 * Provides a handler for sub. consumer that puts a t-stream envelope in a persistent blocking queue
 *
 * @author Kseniya Mikhaleva
 * @param blockingQueue Persistent blocking queue for storing transactions
 */

class ConsumerCallback(blockingQueue: PersistentBlockingQueue) extends Callback[Array[Byte]] {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val envelopeSerializer = new JsonSerializer()

  override def onTransaction(operator: TransactionOperator[Array[Byte]], transaction: ConsumerTransaction[Array[Byte]]) = {
    val consumer = operator.asInstanceOf[Consumer[Array[Byte]]]
    logger.debug(s"onTransaction handler was invoked by subscriber: ${consumer.name}\n")
    val stream = ConnectionRepository.getStreamService.get(consumer.stream.getName).get

    val envelope = new TStreamEnvelope()
    envelope.stream = stream.name
    envelope.partition = transaction.getPartition()
    envelope.id = transaction.getTransactionID()
    envelope.consumerName = consumer.name
    envelope.data = transaction.getAll()
    envelope.tags = stream.tags

    blockingQueue.put(envelopeSerializer.serialize(envelope))
  }
}
