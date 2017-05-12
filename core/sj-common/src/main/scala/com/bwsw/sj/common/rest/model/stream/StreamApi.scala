package com.bwsw.sj.common.rest.model.stream

import com.bwsw.sj.common.si.model.stream._
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSubTypes, JsonTypeInfo}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = classOf[StreamApi], visible = true)
@JsonSubTypes(Array(
  new Type(value = classOf[TStreamStreamApi], name = StreamLiterals.tstreamType),
  new Type(value = classOf[KafkaStreamApi], name = StreamLiterals.kafkaStreamType),
  new Type(value = classOf[ESStreamApi], name = StreamLiterals.esOutputType),
  new Type(value = classOf[JDBCStreamApi], name = StreamLiterals.jdbcOutputType),
  new Type(value = classOf[RestStreamApi], name = StreamLiterals.restOutputType)
))
class StreamApi(@JsonProperty("type") val streamType: String,
                val name: String,
                val service: String,
                val tags: Array[String] = Array(),
                val force: Boolean = false,
                val description: String = RestLiterals.defaultDescription) {

  @JsonIgnore
  def to: SjStream =
    new SjStream(streamType, name, service, tags, force, description)
}

object StreamApi {

  def from(stream: SjStream): StreamApi = stream.streamType match {
    case StreamLiterals.tstreamType =>
      val tStreamStream = stream.asInstanceOf[TStreamStream]

      new TStreamStreamApi(
        tStreamStream.name,
        tStreamStream.service,
        tStreamStream.tags,
        tStreamStream.force,
        tStreamStream.description,
        tStreamStream.partitions
      )

    case StreamLiterals.kafkaStreamType =>
      val kafkaStream = stream.asInstanceOf[KafkaStream]

      new KafkaStreamApi(
        kafkaStream.name,
        kafkaStream.service,
        kafkaStream.tags,
        kafkaStream.force,
        kafkaStream.description,
        kafkaStream.partitions,
        kafkaStream.replicationFactor
      )

    case StreamLiterals.esOutputType =>
      val esStream = stream.asInstanceOf[ESStream]

      new ESStreamApi(
        esStream.name,
        esStream.service,
        esStream.tags,
        esStream.force,
        esStream.description
      )

    case StreamLiterals.restOutputType =>
      val restStream = stream.asInstanceOf[RestStream]

      new RestStreamApi(
        restStream.name,
        restStream.service,
        restStream.tags,
        restStream.force,
        restStream.description
      )

    case StreamLiterals.jdbcOutputType =>
      val jdbcStream = stream.asInstanceOf[JDBCStream]

      new JDBCStreamApi(
        jdbcStream.primary,
        jdbcStream.name,
        jdbcStream.service,
        jdbcStream.tags,
        jdbcStream.force,
        jdbcStream.description
      )
  }
}
