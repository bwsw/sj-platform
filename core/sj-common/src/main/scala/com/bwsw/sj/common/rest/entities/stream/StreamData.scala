package com.bwsw.sj.common.rest.entities.stream

import com.bwsw.sj.common.DAL.model.stream.SjStream
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils
import com.bwsw.sj.common.utils.{MessageResourceUtils, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSubTypes, JsonTypeInfo}

import scala.collection.mutable.ArrayBuffer

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = classOf[StreamData], visible = true)
@JsonSubTypes(Array(
  new Type(value = classOf[TStreamStreamData], name = StreamLiterals.tstreamType),
  new Type(value = classOf[KafkaStreamData], name = StreamLiterals.kafkaStreamType),
  new Type(value = classOf[ESStreamData], name = StreamLiterals.esOutputType),
  new Type(value = classOf[JDBCStreamData], name = StreamLiterals.jdbcOutputType),
  new Type(value = classOf[RestStreamData], name = StreamLiterals.restOutputType)
))
class StreamData() extends ValidationUtils with MessageResourceUtils {
  @JsonProperty("type") var streamType: String = _
  var name: String = _
  var description: String = "No description"
  var service: String = _
  var tags: Array[String] = Array()
  var force: Boolean = false

  @JsonIgnore
  def create(): Unit = {}

  @JsonIgnore
  def asModelStream(): SjStream = ???

  @JsonIgnore
  def validate() = validateGeneralFields()

  @JsonIgnore
  protected def validateGeneralFields() = {
    val streamDAO = ConnectionRepository.getStreamService
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








