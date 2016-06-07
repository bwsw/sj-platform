package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.common.exceptions.BadRecordWithKey
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.StreamConstants
import com.bwsw.sj.crud.rest.entities._
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.stream.StreamValidator

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
            val nameStream = saveStream(stream)
            val response = ProtocolResponse(200, Map("message" -> s"Stream '$nameStream' is created"))
            ctx.complete(HttpEntity(`application/json`, serializer.serialize(response)))
          } else {
            throw new BadRecordWithKey(
              s"Cannot create stream. Errors: ${errors.mkString("\n")}",
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
            response = ProtocolResponse(200, Map("message" -> "No streams found"))
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
              response = ProtocolResponse(200, Map("message" -> s"Stream '$streamName' not found"))
            }
            complete(HttpEntity(`application/json`, serializer.serialize(response)))
          }
        }
      }
    }
  }

  /**
    * Represent SjStream object as SjStreamData object
    *
    * @param stream - SjStream object
    * @return - SjStreamData object
    */
  def streamToStreamData(stream: SjStream) = {
    var streamData: SjStreamData = null
    stream match {
      case s: TStreamSjStream =>
        streamData = new TStreamSjStreamData
        val generatorType = stream.asInstanceOf[TStreamSjStream].generator.generatorType
        val generator = new GeneratorData(
          generatorType,
          if (generatorType != "local") stream.asInstanceOf[TStreamSjStream].generator.service.name else null,
          if (generatorType != "local") stream.asInstanceOf[TStreamSjStream].generator.instanceCount else 0
        )
        streamData.asInstanceOf[TStreamSjStreamData].partitions = stream.asInstanceOf[TStreamSjStream].partitions
        streamData.asInstanceOf[TStreamSjStreamData].generator = generator
      case s: KafkaSjStream =>
        streamData = new KafkaSjStreamData
        streamData.asInstanceOf[KafkaSjStreamData].partitions = stream.asInstanceOf[KafkaSjStream].partitions
        streamData.asInstanceOf[KafkaSjStreamData].replicationFactor = stream.asInstanceOf[KafkaSjStream].replicationFactor
      case s: ESSjStream =>
        streamData = new ESSjStreamData
      case s: JDBCSjStream =>
        streamData = new JDBCSjStreamData
    }
    streamData.name = stream.name
    streamData.description = stream.description
    streamData.service = stream.service.name
    streamData.streamType = stream.streamType
    streamData.tags = stream.tags
    streamData
  }

  /**
    * Save stream to db
    *
    * @param stream - stream entity
    * @return - name of saved entity
    */
  def saveStream(stream: SjStream) = {
    streamDAO.save(stream)
    stream.name
  }
}
