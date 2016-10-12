package com.bwsw.sj.common.rest.entities.stream

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils
import com.bwsw.sj.common.utils.StreamLiterals
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSubTypes, JsonTypeInfo}

import scala.collection.mutable.ArrayBuffer

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "stream-type")
@JsonSubTypes(Array(
  new Type(value = classOf[TStreamSjStreamData], name = StreamLiterals.tStreamType),
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
        errors += s"'Name' is required"
      case Some(x) =>
        val streamObj = streamDAO.get(x)
        if (streamObj.isDefined) {
          errors += s"Stream with name $x already exists"
        }

        if (!validateName(x)) {
          errors += s"Stream has incorrect name: '$x'. " +
            s"Name of stream must contain digits, lowercase letters or hyphens. First symbol must be a letter"
        }
    }

    Option(this.streamType) match {
      case Some(t) =>
        if (!StreamLiterals.types.contains(t)) {
          errors += s"Unknown type '$t' provided. Must be one of: ${StreamLiterals.types.mkString("[", ", ", "]")}"
        }
      case None =>
        errors += s"'Type' is required"
    }

    errors
  }
}








