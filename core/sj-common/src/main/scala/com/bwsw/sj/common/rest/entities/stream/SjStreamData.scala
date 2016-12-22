package com.bwsw.sj.common.rest.entities.stream

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils
import com.bwsw.sj.common.utils.StreamLiterals
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSubTypes, JsonTypeInfo}

import scala.collection.mutable.ArrayBuffer

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "stream-type", defaultImpl = classOf[SjStreamData], visible = true)
@JsonSubTypes(Array(
  new Type(value = classOf[TStreamSjStreamData], name = StreamLiterals.tstreamType),
  new Type(value = classOf[KafkaSjStreamData], name = StreamLiterals.kafkaStreamType),
  new Type(value = classOf[ESSjStreamData], name = StreamLiterals.esOutputType),
  new Type(value = classOf[JDBCSjStreamData], name = StreamLiterals.jdbcOutputType)
))
class SjStreamData() extends ValidationUtils {
  @JsonProperty("stream-type") var streamType: String = null
  var name: String = null
  var description: String = "No description"
  var service: String = null
  var tags: Array[String] = Array()
  var force: Boolean = false

  @JsonIgnore
  def create(): Unit = {}

  @JsonIgnore
  def asModelStream(): SjStream = ???

  @JsonIgnore
  protected def fillModelStream(stream: SjStream) = {
    val serviceDAO = ConnectionRepository.getServiceManager
    stream.name = this.name
    stream.description = this.description
    stream.service = serviceDAO.get(this.service).get
    stream.tags = this.tags
    stream.streamType = this.streamType
  }

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
      case Some(t) =>
        if (!StreamLiterals.types.contains(t)) {
          errors += createMessage("entity.error.unknown.type.must.one.of", t, "stream", StreamLiterals.types.mkString("[", ", ", "]"))
        }
      case None =>
        errors += createMessage("entity.error.attribute.required", "Type")
    }

    errors
  }
}








