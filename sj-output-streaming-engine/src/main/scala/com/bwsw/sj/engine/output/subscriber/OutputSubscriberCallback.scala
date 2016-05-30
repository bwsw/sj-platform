package com.bwsw.sj.engine.output.subscriber

import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.entities.TStreamEnvelope
import com.bwsw.tstreams.agents.consumer.subscriber.{BasicSubscriberCallback, BasicSubscribingConsumer}

/**
  * Created: 27/05/2016
  *
  * @author Kseniya Tomskikh
  */
class OutputSubscriberCallback(blockingQueue: ArrayBlockingQueue[String]) extends BasicSubscriberCallback[Array[Byte], Array[Byte]] {

  private val serializer = new JsonSerializer()

  override def onEvent(subscriber: BasicSubscribingConsumer[Array[Byte], Array[Byte]],
                       partition: Int,
                       transactionUuid: UUID): Unit = {
    val txn = subscriber.getTransactionById(partition, transactionUuid).get
    val stream = ConnectionRepository.getStreamService.get(subscriber.stream.getName)
    val envelope = new TStreamEnvelope()
    envelope.stream = stream.name
    envelope.partition = partition
    envelope.txnUUID = transactionUuid
    envelope.consumerName = subscriber.name
    envelope.data = txn.getAll()
    envelope.tags = stream.tags
    blockingQueue.put(serializer.serialize(envelope))
  }

  override val frequency: Int = 1
}
