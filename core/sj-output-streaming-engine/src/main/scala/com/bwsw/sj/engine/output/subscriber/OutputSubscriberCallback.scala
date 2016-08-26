package com.bwsw.sj.engine.output.subscriber

import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.entities.TStreamEnvelope
import com.bwsw.tstreams.agents.consumer.subscriber.{SubscribingConsumer, Callback}
import org.slf4j.{Logger, LoggerFactory}

/**
  * Subscriber callback for processing transaction of t-stream
  * for output-streaming engine
  *
  *
  *
  * @author Kseniya Tomskikh
  * @param blockingQueue Blocking Queue for saving new transaction from t-stream
  */
class OutputSubscriberCallback(blockingQueue: ArrayBlockingQueue[String])
  extends Callback[Array[Byte]] {

  private val logger: Logger = LoggerFactory.getLogger(getClass)
  private val envelopeSerializer = new JsonSerializer()

  /**
    * Executing, when getting new transaction from t-stream
    * Putting new transaction as envelope to blocking queue
    *
    * @param subscriber Subscriber for consumer for read data from t-stream
    * @param partition Number of partition
    * @param transactionUuid Txn uuid from t-stream
    */
  override def onEvent(subscriber: SubscribingConsumer[Array[Byte]],
                       partition: Int,
                       transactionUuid: UUID): Unit = {
    logger.debug(s"onEvent handler was invoked by subscriber: ${subscriber.name}\n")
    val txn = subscriber.getTransactionById(partition, transactionUuid).get
    val stream = ConnectionRepository.getStreamService.get(subscriber.stream.getName).get
    val envelope = new TStreamEnvelope()
    envelope.stream = stream.name
    envelope.partition = partition
    envelope.txnUUID = transactionUuid
    envelope.consumerName = subscriber.name
    envelope.data = txn.getAll()
    envelope.tags = stream.tags

    blockingQueue.put(envelopeSerializer.serialize(envelope))
  }
}
