package com.bwsw.sj.common.module

import java.util.UUID

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.module.entities.Transaction
import com.bwsw.tstreams.agents.consumer.{BasicConsumerCallback, BasicConsumerWithSubscribe}

/**
 * Provides a handler for sub. consumer that puts a transaction in a persistent blocking queue
 * Created: 21/04/2016
 * @author Kseniya Mikhaleva

 * @param blockingQueue Persistent blocking queue for storing transactions
 */

class QueueConsumerCallback[DATATYPE, USERTYPE](blockingQueue: PersistentBlockingQueue) extends BasicConsumerCallback[DATATYPE, USERTYPE] {
  /**
   * Provides a serialization from Transaction to String in order to put in queue
   */
  private val serializer = new JsonSerializer()

  /**
   * How much times onEvent handler is invoked
   */
  override val frequency: Int = 1

  override def onEvent(subscriber: BasicConsumerWithSubscribe[DATATYPE, USERTYPE], partition: Int, transactionUuid: UUID): Unit = {
    val transaction = subscriber.getTransactionById(partition, transactionUuid).get
    blockingQueue.put(serializer.serialize(
      Transaction(subscriber.stream.getName,
        partition,
        transactionUuid,
        "callback_consumer",
        transaction.getAll().asInstanceOf[List[Array[Byte]]]
      )))
    subscriber.setLocalOffset(partition, transactionUuid)
  }
}
