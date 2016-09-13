package com.bwsw.sj.crud.rest.api

import java.text.MessageFormat

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.rest.entities._
import com.bwsw.sj.common.rest.entities.stream.SjStreamData
import com.bwsw.sj.common.utils.EngineConstants._
import com.bwsw.sj.crud.rest.utils.{CompletionUtils, StreamUtil}
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

import scala.collection.mutable

trait SjStreamsApi extends Directives with SjCrudValidator with CompletionUtils {

  val streamsApi = {
    pathPrefix("streams") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          val data: SjStreamData = serializer.deserialize[SjStreamData](getEntityFromContext(ctx))
          val errors = data.validate()
          var response: RestResponse = BadRequestRestResponse(Map("message" ->
            MessageFormat.format(messages.getString("rest.streams.stream.cannot.create"), errors.mkString(";"))))
          if (errors.isEmpty) {
            val stream = data.asModelStream()
            errors ++= StreamUtil.chackAndCreate(data, stream)
            response = BadRequestRestResponse(Map("message" ->
            MessageFormat.format(messages.getString("rest.streams.stream.cannot.create"), errors.mkString(";"))))
            if (errors.isEmpty) {
              streamDAO.save(stream)
              response = CreatedRestResponse(Map("message" ->
                MessageFormat.format(messages.getString("rest.streams.stream.created"), data.name))
              )
            }
          }

          ctx.complete(restResponseToHttpResponse(response))
        } ~
          get {
            val streams = streamDAO.getAll
            var response: RestResponse = NotFoundRestResponse(Map("message" -> messages.getString("rest.streams.notfound")))
            if (streams.nonEmpty) {
              val entity = Map("streams" -> streams.map(s => s.asProtocolStream()))
              response = OkRestResponse(entity)
            }

            complete(restResponseToHttpResponse(response))
          }
      } ~
        pathPrefix(Segment) { (streamName: String) =>
          pathEndOrSingleSlash {
            get {
              val stream = streamDAO.get(streamName)
              var response: RestResponse = NotFoundRestResponse(Map("message" ->
                MessageFormat.format(messages.getString("rest.streams.stream.notfound"), streamName))
              )
              stream match {
                case Some(x) =>
                  val entity = Map("streams" -> x.asProtocolStream())
                  response = OkRestResponse(entity)
                case None =>
              }

              complete(restResponseToHttpResponse(response))
            } ~
              delete {
                var response: RestResponse = UnprocessableEntityRestResponse(Map("message" ->
                  MessageFormat.format(messages.getString("rest.streams.stream.cannot.delete"), streamName)))

                val instances = getUsedInstances(streamName)

                if (instances.isEmpty) {
                  val stream = streamDAO.get(streamName)
                  stream match {
                    case Some(x) =>
                      StreamUtil.deleteStream(x)
                      streamDAO.delete(streamName)
                      response = OkRestResponse(Map("message" ->
                        MessageFormat.format(messages.getString("rest.streams.stream.deleted"), streamName))
                      )
                    case None =>
                      response = NotFoundRestResponse(Map("message" ->
                        MessageFormat.format(messages.getString("rest.streams.stream.notfound"), streamName))
                      )
                  }
                }

                complete(restResponseToHttpResponse(response))
              }
          }
        }
    }
  }

  private def getUsedInstances(streamName: String): mutable.Buffer[Instance] = {
    instanceDAO.getAll.filter { (instance: Instance) =>
      if (!instance.moduleType.equals(inputStreamingType)) {
        instance.inputs.map(_.replaceAll("/split|/full", "")).contains(streamName) || instance.outputs.contains(streamName)
      } else {
        instance.outputs.contains(streamName)
      }
    }
  }
}
