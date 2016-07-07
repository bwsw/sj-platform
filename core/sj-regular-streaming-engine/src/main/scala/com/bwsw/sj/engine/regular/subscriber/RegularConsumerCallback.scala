package com.bwsw.sj.engine.regular.subscriber

import java.util.UUID

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.PersistentBlockingQueue
import com.bwsw.sj.engine.core.entities.TStreamEnvelope
import com.bwsw.tstreams.agents.consumer.subscriber.{BasicSubscriberCallback, BasicSubscribingConsumer}
import org.slf4j.LoggerFactory


/**
  * Provides a handler for sub. consumer that puts a t-stream envelope in a persistent blocking queue
  * Created: 21/04/2016
  *
  * @author Kseniya Mikhaleva
  * @param blockingQueue Persistent blocking queue for storing transactions
  */

class RegularConsumerCallback[DATATYPE, USERTYPE](blockingQueue: PersistentBlockingQueue) extends BasicSubscriberCallback[DATATYPE, USERTYPE] {
  private val logger = LoggerFactory.getLogger(this.getClass)
  /**
   * Provides a serialization from Transaction to String in order to put in queue
   */
  private val serializer = new JsonSerializer()

  /**
   * Interval between checking new updates in stream/partition in ms
   */
  override val pollingFrequency: Int = 10

  override def onEvent(subscriber: BasicSubscribingConsumer[DATATYPE, USERTYPE], partition: Int, transactionUuid: UUID): Unit = {
    logger.debug(s"onEvent handler was invoked by subscriber: ${subscriber.name}\n")
    val transaction = subscriber.getTransactionById(partition, transactionUuid).get
    val stream = ConnectionRepository.getStreamService.get(subscriber.stream.getName)
    blockingQueue.put(serializer.serialize(
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
