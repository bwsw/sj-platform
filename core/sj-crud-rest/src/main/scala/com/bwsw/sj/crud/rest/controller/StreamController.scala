package com.bwsw.sj.crud.rest.controller

import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.rest._
import com.bwsw.sj.crud.rest.model.stream.StreamApi
import com.bwsw.sj.common.si.StreamSI
import com.bwsw.sj.common.utils.MessageResourceUtils.{createMessage, createMessageWithErrors}
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.crud.rest.{RelatedToStreamResponseEntity, StreamResponseEntity, StreamsResponseEntity}
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator

import scala.util.{Failure, Success, Try}

class StreamController extends Controller {

  override val serviceInterface = new StreamSI

  override def create(serializedEntity: String): RestResponse = {
    Try(serializer.deserialize[StreamApi](serializedEntity)) match {
      case Success(streamData) =>
        serviceInterface.create(streamData.to) match {
          case Right(_) =>
            CreatedRestResponse(
              MessageResponseEntity(
                createMessage("rest.streams.stream.created", streamData.name)))
          case Left(errors) =>
            BadRequestRestResponse(
              MessageResponseEntity(
                createMessageWithErrors("rest.streams.stream.cannot.create", errors)))
        }

      case Failure(exception: JsonDeserializationException) =>
        val error = JsonDeserializationErrorMessageCreator(exception)
        BadRequestRestResponse(
          MessageResponseEntity(
            createMessage("rest.streams.stream.cannot.create", error)))

      case Failure(exception) => throw exception
    }
  }

  override def get(name: String): RestResponse = {
    serviceInterface.get(name) match {
      case Some(stream) =>
        OkRestResponse(StreamResponseEntity(StreamApi.from(stream)))
      case None =>
        NotFoundRestResponse(
          MessageResponseEntity(
            createMessage("rest.streams.stream.notfound", name)))
    }
  }

  override def getAll(): RestResponse = {
    val streams = serviceInterface.getAll()
    val responseEntity = StreamsResponseEntity(streams.map(StreamApi.from))
    OkRestResponse(responseEntity)
  }

  def getTypes =
    OkRestResponse(TypesResponseEntity(StreamLiterals.types))

  def getRelated(name: String): RestResponse = {
    serviceInterface.getRelated(name) match {
      case Some(related) =>
        OkRestResponse(RelatedToStreamResponseEntity(related))
      case None =>
        NotFoundRestResponse(
          MessageResponseEntity(
            createMessage("rest.streams.stream.notfound", name)))
    }
  }

  override def delete(name: String): RestResponse = {
    serviceInterface.delete(name) match {
      case Right(true) =>
        OkRestResponse(
          MessageResponseEntity(
            createMessage("rest.streams.stream.deleted", name)))
      case Right(false) =>
        NotFoundRestResponse(
          MessageResponseEntity(
            createMessage("rest.streams.stream.notfound", name)))
      case Left(message) =>
        UnprocessableEntityRestResponse(MessageResponseEntity(message))
    }
  }
}
