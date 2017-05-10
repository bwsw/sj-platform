package com.bwsw.sj.common.rest.model.stream

import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils._
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.StreamLiterals
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
class StreamApi() {
  @JsonProperty("type") var streamType: String = null
  var name: String = null
  var description: String = "No description"
  var service: String = null
  var tags: Array[String] = Array()
  var force: Boolean = false

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








