package com.bwsw.sj.engine.output.subscriber

import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.entities.TStreamEnvelope
import com.bwsw.tstreams.agents.consumer.{Consumer, TransactionOperator}
import com.bwsw.tstreams.agents.consumer.subscriber.Callback
import org.slf4j.{Logger, LoggerFactory}

/**
 * Subscriber callback for processing transaction of t-stream
 * for output-streaming engine
 *
 * @param blockingQueue Blocking Queue for saving new transaction from t-stream
 *
 * @author Kseniya Tomskikh
 */
class OutputSubscriberCallback(blockingQueue: ArrayBlockingQueue[String])
  extends Callback[Array[Byte]] {

  private val logger: Logger = LoggerFactory.getLogger(getClass)
  private val envelopeSerializer = new JsonSerializer()

  /**
   * Executing, when getting new transaction from t-stream
   * Putting new transaction as envelope to blocking queue
   *
   * @param operator Subscriber for consumer for read data from t-stream
   * @param partition Number of partitions
   * @param uuid Txn uuid from t-stream
   */
  override def onEvent(operator: TransactionOperator[Array[Byte]], partition: Int, uuid: UUID, count: Int): Unit = {
    val consumer = operator.asInstanceOf[Consumer[Array[Byte]]]
    logger.debug(s"onEvent handler was invoked by subscriber: ${consumer.name}\n")
    val txn = consumer.getTransactionById(partition, uuid).get
    val stream = ConnectionRepository.getStreamService.get(consumer.stream.getName).get
    val envelope = new TStreamEnvelope()
    envelope.stream = stream.name
    envelope.partition = partition
    envelope.txnUUID = uuid
    envelope.consumerName = consumer.name
    envelope.data = txn.getAll()
    envelope.tags = stream.tags

    blockingQueue.put(envelopeSerializer.serialize(envelope))
  }
}
