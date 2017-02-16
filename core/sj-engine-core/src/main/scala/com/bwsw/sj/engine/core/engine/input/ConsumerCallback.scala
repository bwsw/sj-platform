package com.bwsw.sj.engine.core.engine.input

import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.entities.{Envelope, TStreamEnvelope}
import com.bwsw.tstreams.agents.consumer.subscriber.Callback
import com.bwsw.tstreams.agents.consumer.{Consumer, ConsumerTransaction, TransactionOperator}
import org.slf4j.LoggerFactory


/**
 * Provides a handler for sub. consumer that puts a t-stream envelope in a persistent blocking queue
 *
 * @author Kseniya Mikhaleva
 * @param blockingQueue Persistent blocking queue for storing transactions
 */

class ConsumerCallback(blockingQueue: ArrayBlockingQueue[Envelope]) extends Callback[Array[Byte]] {
  private val logger = LoggerFactory.getLogger(this.getClass)

  override def onTransaction(operator: TransactionOperator[Array[Byte]], transaction: ConsumerTransaction[Array[Byte]]) = {
    val consumer = operator.asInstanceOf[Consumer[Array[Byte]]]
    logger.debug(s"onTransaction handler was invoked by subscriber: ${consumer.name}.")
    val stream = ConnectionRepository.getStreamService.get(consumer.stream.name).get

    val envelope = new TStreamEnvelope(transaction.getAll(), consumer.name)
    envelope.stream = stream.name
    envelope.partition = transaction.getPartition()
    envelope.tags = stream.tags
    envelope.id = transaction.getTransactionID()

    blockingQueue.put(envelope)
  }
}
