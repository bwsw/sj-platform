package com.bwsw.sj.engine.core

import java.util.UUID

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.entities.TStreamEnvelope
import com.bwsw.tstreams.agents.consumer.subscriber.{Callback, SubscribingConsumer}
import org.slf4j.LoggerFactory


/**
 * Provides a handler for sub. consumer that puts a t-stream envelope in a persistent blocking queue
 *
 *
 * @author Kseniya Mikhaleva
 * @param blockingQueue Persistent blocking queue for storing transactions
 */

class ConsumerCallback(blockingQueue: PersistentBlockingQueue) extends Callback[Array[Byte]] {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val envelopeSerializer = new JsonSerializer()

  override def onEvent(subscriber: SubscribingConsumer[Array[Byte]], partition: Int, transactionUuid: UUID): Unit = {
    logger.debug(s"onEvent handler was invoked by subscriber: ${subscriber.name}\n")
    val transaction = subscriber.getTransactionById(partition, transactionUuid).get
    val stream = ConnectionRepository.getStreamService.get(subscriber.stream.getName).get

    val envelope = new TStreamEnvelope()
    envelope.stream = stream.name
    envelope.partition = partition
    envelope.txnUUID = transactionUuid
    envelope.consumerName = subscriber.name
    envelope.data = transaction.getAll()
    envelope.tags = stream.tags

    blockingQueue.put(envelopeSerializer.serialize(envelope))
  }
}
