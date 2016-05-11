package com.bwsw.sj.common.module.entities

import java.util.UUID

/**
 * Represents a message envelope that is received by an Executor for each message
 * that is received from a partition of a specific input kafka stream or t-stream.
 */
class Envelope() {
  var streamType: String = null
  var stream: String = null
  var partition: Int = 0
  var tags: String = null
}

/**
 * Provides a wrapper for t-stream transaction.
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva

 * @param _stream Stream name from which a transaction received
 * @param _partition Number of stream partition from which a transaction received
 * @param txnUUID Transaction UUID
 * @param consumerName Name of consumer extracted a transaction
 * @param data Transaction data
 * @param _tags Tags of t-stream
 */

class TStreamEnvelope(_stream: String,
                           _partition: Int,
                           var txnUUID: UUID,
                           var consumerName: String,
                           var data: List[Array[Byte]],
                           _tags: String)
  extends Envelope() {
  stream = _stream
  streamType = "t-stream"
  partition = _partition
  tags = _tags
}

/**
 * Provides a wrapper for kafka message.
 * @param _stream Stream name from which a message received
 * @param _partition Number of stream partition from which a message received
 * @param data Message
 * @param offset Message offset
 * @param _tags Tags of kafka stream
 */
case class KafkaEnvelope(_stream: String,
                         _partition: Int,
                         var data: Array[Byte],
                         var offset: Long,
                         _tags: String)
  extends Envelope() {
  stream = _stream
  streamType = "kafka-stream"
  partition = _partition
  tags = _tags
}