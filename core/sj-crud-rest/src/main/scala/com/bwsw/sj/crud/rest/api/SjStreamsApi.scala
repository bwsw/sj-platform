package com.bwsw.sj.crud.rest.api

import java.text.MessageFormat

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.ModuleConstants._
import com.bwsw.sj.common.StreamConstants
import com.bwsw.sj.crud.rest.entities._
import com.bwsw.sj.crud.rest.entities.stream.SjStreamData
import com.bwsw.sj.crud.rest.utils.ConvertUtil.streamToStreamData
import com.bwsw.sj.crud.rest.utils.{CompletionUtils, StreamUtil}
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.stream.StreamValidator

import scala.collection.mutable

/**
 * Rest-api for streams
 *
 * Created by mendelbaum_nm
 */
trait SjStreamsApi extends Directives with SjCrudValidator with CompletionUtils {

  val streamsApi = {
    pathPrefix("streams") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          val data: SjStreamData = serializer.deserialize[SjStreamData](getEntityFromContext(ctx))

          val stream = createStream(data)
          val errors = StreamValidator.validate(data, stream)
          var response: RestResponse = BadRequestRestResponse(Map("message" ->
            MessageFormat.format(messages.getString("rest.streams.stream.cannot.create"), errors.mkString("\n"))))
          if (errors.isEmpty) {
            streamDAO.save(stream)
            response = CreatedRestResponse(Map("message" ->
              MessageFormat.format(messages.getString("rest.streams.stream.created"), stream.name))
            )
          }

          ctx.complete(restResponseToHttpResponse(response))
        } ~
          get {
            val streams = streamDAO.getAll
            var response: RestResponse = NotFoundRestResponse(Map("message" -> messages.getString("rest.streams.notfound")))
            if (streams.nonEmpty) {
              val entity = Map("streams" -> streams.map(s => streamToStreamData(s)))
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
                  val entity = Map("streams" -> streamToStreamData(x))
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

  def createStream(data: SjStreamData) = {
    var stream = new SjStream
    data.streamType match {
      case StreamConstants.`tStreamType` =>
        stream = new TStreamSjStream
        stream.streamType = StreamConstants.tStreamType
      case StreamConstants.`kafkaStreamType` =>
        stream = new KafkaSjStream
        stream.streamType = StreamConstants.kafkaStreamType
      case StreamConstants.`esOutputType` =>
        stream = new ESSjStream
        stream.streamType = StreamConstants.esOutputType
      case StreamConstants.`jdbcOutputType` =>
        stream = new JDBCSjStream
        stream.streamType = StreamConstants.jdbcOutputType
    }

    stream
  }

  def getUsedInstances(streamName: String): mutable.Buffer[Instance] = {
    instanceDAO.getAll.filter { (instance: Instance) =>
      if (!instance.moduleType.equals(inputStreamingType)) {
        instance.inputs.map(_.replaceAll("/split|/full", "")).contains(streamName) || instance.outputs.contains(streamName)
      } else {
        instance.outputs.contains(streamName)
      }
    }
  }
}
