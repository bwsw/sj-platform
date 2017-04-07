package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.rest.entities._
import com.bwsw.sj.common.rest.entities.stream.StreamData
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

trait SjStreamsApi extends Directives with SjCrudValidator {

  val streamsApi = {
    pathPrefix("streams") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          validateContextWithSchema(ctx, "streamSchema.json")
          val streamData = serializer.deserialize[StreamData](getEntityFromContext(ctx))
          val errors = streamData.validate()
          var response: RestResponse = BadRequestRestResponse(MessageResponseEntity(
            createMessage("rest.streams.stream.cannot.create", errors.mkString(";"))
          ))

          if (errors.isEmpty) {
            streamData.create()
            streamDAO.save(streamData.asModelStream())
            response = CreatedRestResponse(MessageResponseEntity(createMessage("rest.streams.stream.created", streamData.name)))
          }

          ctx.complete(restResponseToHttpResponse(response))
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
