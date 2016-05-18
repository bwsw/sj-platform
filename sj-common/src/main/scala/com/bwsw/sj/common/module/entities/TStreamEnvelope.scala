package com.bwsw.sj.common.module.entities

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import org.slf4j.LoggerFactory

/**
 * Represents a message envelope that is received by an Executor for each message
 * that is received from a partition of a specific input kafka stream or t-stream.
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "streamType")
@JsonSubTypes(Array(
  new Type(value = classOf[TStreamEnvelope], name = "t-stream"),
  new Type(value = classOf[KafkaEnvelope], name = "kafka-stream")
))
class Envelope() {
  protected val logger = LoggerFactory.getLogger(this.getClass)

  var streamType: String = null
  var stream: String = null
  var partition: Int = 0
  var tags: Array[String] = null
}

/**
 * Provides a wrapper for t-stream transaction.
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

class TStreamEnvelope() extends Envelope() {
  logger.info(s"Create t-stream envelope\n")

  var txnUUID: UUID = null
  var consumerName: String = null
  var data: List[Array[Byte]] = null
  streamType = "t-stream"
}

/**
 * Provides a wrapper for kafka message.
 */
class KafkaEnvelope() extends Envelope() {
  logger.info(s"Create kafka envelope\n")

  var data: Array[Byte] = null
  var offset: Long = 0
  streamType = "kafka-stream"

}