package com.bwsw.sj.common.module.entities

import java.util.UUID

/**
 * Represents a message envelope that is received by an Executor for each message
 * that is received from a partition of a specific input kafka stream or t-stream.
 */
trait Envelope {
  val streamType: String
  val stream: String
  val partition: Int
}

/**
 * Provides a wrapper for t-stream transaction.
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva

 * @param stream Stream name from which a transaction received
 * @param partition Number of stream partition from which a transaction received
 * @param txnUUID Transaction UUID
 * @param consumerID Consumer extracted a transaction
 * @param data Transaction data
 */

case class TStreamEnvelope(stream: String, partition: Int, txnUUID: UUID, consumerID: String, data: List[Array[Byte]], streamType: String = "t-stream") extends Envelope

/**
 * Provides a wrapper for kafka message.
 * @param stream Stream name from which a message received
 * @param partition Number of stream partition from which a message received
 * @param data Message
 */
case class KafkaEnvelope(stream: String, partition: Int, data: Array[Byte], streamType: String = "kafka-stream") extends Envelope