package com.bwsw.sj.engine.core.entities


/**
 * Represents a message envelope that is received by an Executor for each message
 * that is received from a partition of a specific input (kafka, t-stream, elasticsearch, jdbc)
 */

//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "streamType", defaultImpl = classOf[Envelope[T]], visible = true)
//@JsonSubTypes(Array(
//  new Type(value = classOf[TStreamEnvelope[T]], name = "stream.t-stream"),
//  new Type(value = classOf[KafkaEnvelope[T]], name = "stream.kafka"),
//  new Type(value = classOf[EsEnvelope], name = "elasticsearch-output"),
//  new Type(value = classOf[JdbcEnvelope], name = "jdbc-output")
//))
class Envelope extends Serializable {
  protected var streamType: String = null
  var stream: String = null
  var partition: Int = 0
  var tags: Array[String] = Array()
  var id: Long = 0

 // @JsonIgnore()
  def isEmpty() = {
    streamType == null
  }
}