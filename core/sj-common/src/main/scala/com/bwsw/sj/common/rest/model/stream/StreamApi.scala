package com.bwsw.sj.common.rest.model.stream

import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils._
import com.bwsw.sj.common.si.model.stream._
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSubTypes, JsonTypeInfo}

import scala.collection.mutable.ArrayBuffer

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

  @JsonIgnore
  def create(): Unit = {}

  @JsonIgnore
  def asModelStream(): StreamDomain = ???

  @JsonIgnore
  def validate(): ArrayBuffer[String] = validateGeneralFields()

  @JsonIgnore
  protected def validateGeneralFields(): ArrayBuffer[String] = {
    val streamDAO = ConnectionRepository.getStreamRepository
    val errors = new ArrayBuffer[String]()

    Option(this.name) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Name")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Name")
        }
        else {
          if (!validateName(x)) {
            errors += createMessage("entity.error.incorrect.name", "Stream", x, "stream")
          }

          val streamObj = streamDAO.get(x)
          if (streamObj.isDefined) {
            errors += createMessage("entity.error.already.exists", "Stream", x)
          }
        }
    }

    Option(this.streamType) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Type")
      case Some(t) =>
        if (t.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Type")
        }
        else {
          if (!StreamLiterals.types.contains(t)) {
            errors += createMessage("entity.error.unknown.type.must.one.of", t, "stream", StreamLiterals.types.mkString("[", ", ", "]"))
          }
        }
    }

    errors
  }
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








