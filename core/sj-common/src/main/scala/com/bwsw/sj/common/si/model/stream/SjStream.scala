package com.bwsw.sj.common.si.model.stream

import com.bwsw.sj.common.dal.model.stream.{RestStreamDomain, StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils.validateName
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage
import com.bwsw.sj.common.utils.StreamLiterals

import scala.collection.mutable.ArrayBuffer

class SjStream(val streamType: String,
               val name: String,
               val service: String,
               val tags: Array[String],
               val force: Boolean,
               val description: String) {

  def to(): StreamDomain = ???

  def validate(): ArrayBuffer[String] = validateGeneralFields()

  protected def validateGeneralFields(): ArrayBuffer[String] = {
    val streamDAO = ConnectionRepository.getStreamRepository
    val errors = new ArrayBuffer[String]()

    // 'name' field
    Option(name) match {
      case Some("") | None =>
        errors += createMessage("entity.error.attribute.required", "Name")
      case Some(x) =>
        if (!validateName(x)) {
          errors += createMessage("entity.error.incorrect.name", "Stream", x, "stream")
        }

        val streamObj = streamDAO.get(x)
        if (streamObj.isDefined) {
          errors += createMessage("entity.error.already.exists", "Stream", x)
        }
    }

    // 'streamType' field
    Option(streamType) match {
      case Some("") | None =>
        errors += createMessage("entity.error.attribute.required", "Type")
      case Some(t) =>
        if (!StreamLiterals.types.contains(t)) {
          errors += createMessage("entity.error.unknown.type.must.one.of", t, "stream", StreamLiterals.types.mkString("[", ", ", "]"))
        }
    }

    errors
  }
}

object SjStream {

  def from(streamDomain: StreamDomain): SjStream = streamDomain.streamType match {
    case StreamLiterals.tstreamType =>
      val tStreamStream = streamDomain.asInstanceOf[TStreamStreamDomain]

      new TStreamStream(
        tStreamStream.name,
        tStreamStream.service.name,
        tStreamStream.partitions,
        tStreamStream.tags,
        tStreamStream.force,
        tStreamStream.streamType,
        tStreamStream.description
      )

    case StreamLiterals.restOutputType =>
      val restStream = streamDomain.asInstanceOf[RestStreamDomain]

      new RestStream(
        restStream.name,
        restStream.service.name,
        restStream.tags,
        restStream.force,
        restStream.streamType,
        restStream.description
      )
  }
}
