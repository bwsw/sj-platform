package com.bwsw.sj.engine.regular.subscriber

import java.util.UUID

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.PersistentBlockingQueue
import com.bwsw.sj.engine.core.entities.TStreamEnvelope
import com.bwsw.tstreams.agents.consumer.subscriber.{SubscribingConsumer, Callback}
import org.slf4j.LoggerFactory

/**
  * Provides a handler for sub. consumer that puts a t-stream envelope in a persistent blocking queue
  * Created: 21/04/2016
  *
  * @author Kseniya Mikhaleva
  * @param blockingQueue Persistent blocking queue for storing transactions
  */

class RegularConsumerCallback[USERTYPE](blockingQueue: PersistentBlockingQueue) extends Callback[USERTYPE] {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val txnSerializer = new JsonSerializer()

  override def onEvent(subscriber: SubscribingConsumer[ USERTYPE], partition: Int, transactionUuid: UUID): Unit = {
    logger.debug(s"onEvent handler was invoked by subscriber: ${subscriber.name}\n")
    val transaction = subscriber.getTransactionById(partition, transactionUuid).get
    val stream = ConnectionRepository.getStreamService.get(subscriber.stream.getName).get
    blockingQueue.put(txnSerializer.serialize(
    {
      val envelope = new TStreamEnvelope()
      envelope.stream = stream.name
      envelope.partition = partition
      envelope.txnUUID = transactionUuid
      envelope.consumerName = subscriber.name
      envelope.data = transaction.getAll().asInstanceOf[List[Array[Byte]]]
      envelope.tags = stream.tags
      envelope
    }
    ))
  }
}