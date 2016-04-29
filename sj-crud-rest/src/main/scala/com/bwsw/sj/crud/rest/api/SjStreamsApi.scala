package com.bwsw.sj.crud.rest.api

import java.io.{File, FileNotFoundException}
import java.net.URI

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.directives.FileInfo
import akka.http.scaladsl.server.{Directives, RequestContext}
import akka.stream.scaladsl._
import com.bwsw.common.exceptions.KeyAlreadyExists
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.crud.rest.entities._
import com.bwsw.sj.crud.rest.entities.SjStreamData
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.stream.StreamValidator

/**
  * Rest-api for streams
  *
  * Created: 28/04/2016
  *
  * @author Nikita Mendelbaum
  */
trait SjStreamsApi extends Directives with SjCrudValidator {

  val streamsApi = {
    pathPrefix("streams") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          val options = serializer.deserialize[SjStreamData](getEntityFromContext(ctx))
          val stream = generateStreamEntity(options)
          val errors = validateStream(stream)
          if (errors.isEmpty) {
            val nameStream = saveStream(stream)
            ctx.complete(HttpEntity(
              `application/json`,
              serializer.serialize(Response(200, nameStream, s"Stream $nameStream is created"))
            ))
          } else {
            throw new KeyAlreadyExists(s"Cannot create stream. Errors: ${errors.mkString("\n")}",
              s"${options.name}")
          }
        }
      }
    }
  }

  /**
    * Generate stream entity from stream data
    *
    * @param parameters - options for stream
    * @return - generated stream entity
    */
  def generateStreamEntity(parameters: SjStreamData) = {
    val service = serviceDAO.get(parameters.service)
    val stream = new SjStream
    stream.service = service
    stream.name = parameters.name
    stream.description = parameters.description
    stream.partitions = parameters.partitions
    stream.tags = parameters.tags
    stream.generator = parameters.generator
    stream
  }

  /**
    * Validation of options for created module instance
    *
    * @param stream - stream
    * @return - list of errors
    */
  def validateStream(stream: SjStream) = {
    val validatorClassName = conf.getString("streams.validator-class")
    val validatorClass = Class.forName(validatorClassName)
    val validator = validatorClass.newInstance().asInstanceOf[StreamValidator]
    validator.validate(stream)
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
