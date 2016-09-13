package com.bwsw.sj.common.rest.entities.stream

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils
import com.bwsw.sj.common.utils.StreamConstants
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonProperty, JsonSubTypes, JsonTypeInfo}

import scala.collection.mutable.ArrayBuffer

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "stream-type")
@JsonSubTypes(Array(
  new Type(value = classOf[TStreamSjStreamData], name = StreamConstants.tStreamType),
  new Type(value = classOf[KafkaSjStreamData], name = StreamConstants.kafkaStreamType),
  new Type(value = classOf[ESSjStreamData], name = StreamConstants.esOutputType),
  new Type(value = classOf[JDBCSjStreamData], name = StreamConstants.jdbcOutputType)
))
class SjStreamData() extends ValidationUtils {
  @JsonProperty("stream-type") var streamType: String = null
  var name: String = null
  var description: String = "No description"
  var service: String = null
  var tags: Array[String] = Array()
  var force: Boolean = false

  def asModelStream(): SjStream = ???

  protected def fillModelStream(stream: SjStream) = {
    val serviceDAO = ConnectionRepository.getServiceManager
    stream.name = this.name
    stream.description = this.description
    stream.service = serviceDAO.get(this.service).get
    stream.tags = this.tags
    stream.streamType = this.streamType
  }

  def validate() = validateGeneralFields()

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
            s"Name of stream must be contain digits, lowercase letters or hyphens. First symbol must be a letter"
        }
    }

    Option(this.streamType) match {
      case Some(t) =>
        if (!StreamConstants.streamTypes.contains(t)) {
          errors += s"Unknown type '$t' provided. Must be one of: ${StreamConstants.streamTypes.mkString("[", ", ", "]")}"
        }
      case None =>
        errors += s"'Type' is required"
    }

    errors
  }
}








