package com.bwsw.sj.crud.rest.model.stream

import com.bwsw.sj.common.si.model.stream._
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

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
                val tags: Option[Array[String]] = Some(Array()),
                @JsonDeserialize(contentAs = classOf[Boolean]) val force: Option[Boolean] = Some(false),
                val description: Option[String] = Some(RestLiterals.defaultDescription)) {

  @JsonIgnore
  def to: SjStream =
    new SjStream(
      streamType,
      name,
      service,
      tags.getOrElse(Array()),
      force.getOrElse(false),
      description.getOrElse(RestLiterals.defaultDescription))
}

object StreamApi {

  def from(stream: SjStream): StreamApi = stream.streamType match {
    case StreamLiterals.tstreamType =>
      val tStreamStream = stream.asInstanceOf[TStreamStream]

      new TStreamStreamApi(
        tStreamStream.name,
        tStreamStream.service,
        Option(tStreamStream.tags),
        Option(tStreamStream.force),
        Option(tStreamStream.description),
        Option(tStreamStream.partitions)
      )

    case StreamLiterals.kafkaStreamType =>
      val kafkaStream = stream.asInstanceOf[KafkaStream]

      new KafkaStreamApi(
        kafkaStream.name,
        kafkaStream.service,
        Option(kafkaStream.tags),
        Option(kafkaStream.force),
        Option(kafkaStream.description),
        Option(kafkaStream.partitions),
        Option(kafkaStream.replicationFactor)
      )

    case StreamLiterals.esOutputType =>
      val esStream = stream.asInstanceOf[ESStream]

      new ESStreamApi(
        esStream.name,
        esStream.service,
        Option(esStream.tags),
        Option(esStream.force),
        Option(esStream.description)
      )

    case StreamLiterals.restOutputType =>
      val restStream = stream.asInstanceOf[RestStream]

      new RestStreamApi(
        restStream.name,
        restStream.service,
        Option(restStream.tags),
        Option(restStream.force),
        Option(restStream.description)
      )

    case StreamLiterals.jdbcOutputType =>
      val jdbcStream = stream.asInstanceOf[JDBCStream]

      new JDBCStreamApi(
        jdbcStream.primary,
        jdbcStream.name,
        jdbcStream.service,
        Option(jdbcStream.tags),
        Option(jdbcStream.force),
        Option(jdbcStream.description)
      )
  }
}
