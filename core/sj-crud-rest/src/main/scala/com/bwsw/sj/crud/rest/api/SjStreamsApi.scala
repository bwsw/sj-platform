package com.bwsw.sj.crud.rest.api

import java.text.MessageFormat

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.common.exceptions.BadRecordWithKey
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.StreamConstants
import com.bwsw.sj.crud.rest.entities._
import com.bwsw.sj.crud.rest.utils.ConvertUtil.streamToStreamData
import com.bwsw.sj.crud.rest.utils.StreamUtil
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.stream.StreamValidator
import kafka.common.TopicExistsException

/**
  * Rest-api for streams
  *
  * Created by mendelbaum_nm
  */
trait SjStreamsApi extends Directives with SjCrudValidator {

  val streamsApi = {
    pathPrefix("streams") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          val data = serializer.deserialize[SjStreamData](getEntityFromContext(ctx))

          var stream = new SjStream
          data.streamType match {
            case StreamConstants.tStream =>
              stream = new TStreamSjStream
              stream.streamType = StreamConstants.tStream
            case StreamConstants.kafka =>
              stream = new KafkaSjStream
              stream.streamType = StreamConstants.kafka
            case StreamConstants.esOutput =>
              stream = new ESSjStream
              stream.streamType = StreamConstants.esOutput
            case StreamConstants.jdbcOutput =>
              stream = new JDBCSjStream
              stream.streamType = StreamConstants.jdbcOutput
          }
          val errors = StreamValidator.validate(data, stream)
          if (errors.isEmpty) {
            stream match {
              case s: TStreamSjStream =>
                val streamCheckResult = StreamUtil.checkAndCreateTStream(s, data.force)
                streamCheckResult match {
                  case Left(err) => errors += err
                  case _ =>
                }
              case s: KafkaSjStream =>
                try {
                  val streamCheckResult = StreamUtil.checkAndCreateKafkaTopic(s, data.force)
                  streamCheckResult match {
                    case Left(err) => errors += err
                    case _ =>
                  }
                } catch {
                  case e: TopicExistsException => errors += MessageFormat.format(
                    messages.getString("rest.streams.create.kafka.cannot"),
                    errors.mkString("\n")
                  )
                }
              case s: ESSjStream =>
                  val streamCheckResult = StreamUtil.checkAndCreateEsStream(s, data.force)
                  streamCheckResult match {
                    case Left(err) => errors += err
                    case _ =>
                  }
              case s: JDBCSjStream =>
                val streamCheckResult = StreamUtil.checkAndCreateJdbcStream(s, data.force)
                streamCheckResult match {
                  case Left(err) => errors += err
                  case _ =>
                }
            }
          }
          if (errors.isEmpty) {
            streamDAO.save(stream)
            val response = ProtocolResponse(200, Map("message" -> MessageFormat.format(
              messages.getString("rest.streams.stream.created"),
              stream.name
            )))
            ctx.complete(HttpEntity(`application/json`, serializer.serialize(response)))
          } else {
            throw new BadRecordWithKey(
              MessageFormat.format(
                messages.getString("rest.streams.stream.cannot.create"),
                errors.mkString("\n")
              ),
              s"${data.name}"
            )
          }
        } ~
        get {
          val streams = streamDAO.getAll
          var response: ProtocolResponse = null
          if (streams.nonEmpty) {
            val entity = Map("streams" -> streams.map(s => streamToStreamData(s)))
            response = ProtocolResponse(200, entity)
          } else {
            response = ProtocolResponse(200, Map("message" -> messages.getString("rest.streams.notfound")))
          }
          complete(HttpEntity(`application/json`, serializer.serialize(response)))

        }
      } ~
      pathPrefix(Segment) { (streamName: String) =>
        pathEndOrSingleSlash {
          get {
            val stream = streamDAO.get(streamName)
            var response: ProtocolResponse = null
            if (stream != null) {
              val entity = Map("streams" -> streamToStreamData(stream))
              response = ProtocolResponse(200, entity)
            } else {
              response = ProtocolResponse(200, Map("message" -> MessageFormat.format(
                messages.getString("rest.streams.stream.notfound"),
                streamName
              )))
            }
            complete(HttpEntity(`application/json`, serializer.serialize(response)))
          } ~
          delete {

            val instances = instanceDAO.getAll.filter { (inst: Instance) =>
              inst.inputs.map(_.replaceAll("/split|/full", "")).contains(streamName) || inst.outputs.contains(streamName)
            }

            if (instances.isEmpty) {
              val stream = streamDAO.get(streamName)

              var response: ProtocolResponse = null
              if (stream != null) {
                StreamUtil.deleteStream(stream)
                streamDAO.delete(streamName)
                response = ProtocolResponse(200, Map("message" -> MessageFormat.format(
                  messages.getString("rest.streams.stream.deleted"),
                  streamName
                )))
              } else {
                response = ProtocolResponse(200, Map("message" -> MessageFormat.format(
                  messages.getString("rest.streams.stream.notfound"),
                  streamName
                )))
              }
              complete(HttpEntity(`application/json`, serializer.serialize(response)))
            } else {
              throw new BadRecordWithKey(MessageFormat.format(
                messages.getString("rest.streams.stream.cannot.delete"),
                streamName), streamName)
            }
          }
        }
      }
    }
  }
}
