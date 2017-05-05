package com.bwsw.sj.crud.rest.routes

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.dal.model.module.Instance
import com.bwsw.sj.common.rest.model._
import com.bwsw.sj.common.rest.model.stream.StreamData
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

import scala.collection.mutable.ArrayBuffer
import com.bwsw.sj.common.rest.utils.ValidationUtils._
import com.bwsw.sj.common.utils.MessageResourceUtils._

trait SjStreamsRoute extends Directives with SjCrudValidator {

  val streamsApi = {
    pathPrefix("streams") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          var response: Option[RestResponse] = None
          val errors = new ArrayBuffer[String]
          try {
            val streamData = serializer.deserialize[StreamData](getEntityFromContext(ctx))
            errors ++= streamData.validate()

            if (errors.isEmpty) {
              streamData.create()
              streamDAO.save(streamData.asModelStream())
              response = Option(
                CreatedRestResponse(
                  MessageResponseEntity(
                    createMessage("rest.streams.stream.created", streamData.name))))
            }
          } catch {
            case e: JsonDeserializationException =>
              errors += JsonDeserializationErrorMessageCreator(e)
          }

          if (errors.nonEmpty) {
            response = Option(
              BadRequestRestResponse(
                MessageResponseEntity(
                  createMessage("rest.streams.stream.cannot.create", errors.mkString(";")))))
          }

          ctx.complete(restResponseToHttpResponse(response.get))
        } ~
          get {
            val streams = streamDAO.getAll
            val response = OkRestResponse(StreamsResponseEntity())
            if (streams.nonEmpty) {
              response.entity = StreamsResponseEntity(streams.map(s => s.asProtocolStream()))
            }

            complete(restResponseToHttpResponse(response))
          }
      } ~
        pathPrefix("_types") {
          pathEndOrSingleSlash {
            get {
              val response = OkRestResponse(TypesResponseEntity(StreamLiterals.types))

              complete(restResponseToHttpResponse(response))
            }
          }
        } ~
        pathPrefix(Segment) { (streamName: String) =>
          pathEndOrSingleSlash {
            get {
              val stream = streamDAO.get(streamName)
              var response: RestResponse = NotFoundRestResponse(MessageResponseEntity(
                createMessage("rest.streams.stream.notfound", streamName))
              )
              stream match {
                case Some(x) =>
                  response = OkRestResponse(StreamResponseEntity(x.asProtocolStream()))
                case None =>
              }

              complete(restResponseToHttpResponse(response))
            } ~
              delete {
                var response: RestResponse = UnprocessableEntityRestResponse(MessageResponseEntity(
                  createMessage("rest.streams.stream.cannot.delete", streamName)))

                val instances = getRelatedInstances(streamName)

                if (instances.isEmpty) {
                  val stream = streamDAO.get(streamName)
                  stream match {
                    case Some(x) =>
                      x.delete()
                      streamDAO.delete(streamName)
                      response = OkRestResponse(MessageResponseEntity(
                        createMessage("rest.streams.stream.deleted", streamName))
                      )
                    case None =>
                      response = NotFoundRestResponse(MessageResponseEntity(
                        createMessage("rest.streams.stream.notfound", streamName))
                      )
                  }
                }

                complete(restResponseToHttpResponse(response))
              }
          } ~
            pathPrefix("related") {
              pathEndOrSingleSlash {
                get {
                  val stream = streamDAO.get(streamName)
                  var response: RestResponse = NotFoundRestResponse(
                    MessageResponseEntity(createMessage("rest.streams.stream.notfound", streamName)))

                  stream match {
                    case Some(x) =>
                      response = OkRestResponse(RelatedToStreamResponseEntity(getRelatedInstances(streamName)))
                    case None =>
                  }

                  complete(restResponseToHttpResponse(response))
                }
              }
            }
        }
    }
  }

  private def getRelatedInstances(streamName: String) = {
    instanceDAO.getAll.filter {
      (instance: Instance) =>
        if (!instance.moduleType.equals(inputStreamingType)) {
          instance.getInputsWithoutStreamMode().contains(streamName) || instance.outputs.contains(streamName)
        } else {
          instance.outputs.contains(streamName)
        }
    }.map(_.name)
  }
}
