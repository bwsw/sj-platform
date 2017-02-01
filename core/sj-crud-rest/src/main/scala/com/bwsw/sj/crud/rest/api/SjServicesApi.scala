package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.sj.common.DAL.model.{SjStream, TStreamSjStream}
import com.bwsw.sj.common.rest.entities._
import com.bwsw.sj.common.rest.entities.service.ServiceData
import com.bwsw.sj.common.utils.{GeneratorLiterals, ServiceLiterals, StreamLiterals}
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

import scala.collection.mutable

trait SjServicesApi extends Directives with SjCrudValidator {

  val servicesApi = {
    pathPrefix("services") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          validateContextWithSchema(ctx, "serviceSchema.json")
          val protocolService = serializer.deserialize[ServiceData](getEntityFromContext(ctx))
          val errors = protocolService.validate()
          var response: RestResponse = BadRequestRestResponse(Map("message" ->
            createMessage("rest.services.service.cannot.create", errors.mkString(";"))))

          if (errors.isEmpty) {
            val service = protocolService.asModelService()
            service.prepare()
            serviceDAO.save(service)
            response = CreatedRestResponse(Map("message" -> createMessage("rest.services.service.created", service.name)))
          }

          ctx.complete(restResponseToHttpResponse(response))
        } ~
          get {
            val services = serviceDAO.getAll
            val response = OkRestResponse(Map("services" -> mutable.Buffer()))
            if (services.nonEmpty) {
              response.entity = Map("services" -> services.map(_.asProtocolService()))
            }

            complete(restResponseToHttpResponse(response))
          }
      } ~
        pathPrefix("types") {
          //todo if a service has a name 'types' then there is a collision
          pathEndOrSingleSlash {
            get {
              val response = OkRestResponse(Map("types" -> ServiceLiterals.types))

              complete(restResponseToHttpResponse(response))
            }
          }
        } ~
        pathPrefix(Segment) { (serviceName: String) =>
          pathEndOrSingleSlash {
            get {
              val service = serviceDAO.get(serviceName)
              var response: RestResponse = NotFoundRestResponse(Map("message" ->
                createMessage("rest.services.service.notfound", serviceName)))
              service match {
                case Some(x) =>
                  val entity = Map("services" -> x.asProtocolService())
                  response = OkRestResponse(entity)
                case None =>
              }

              complete(restResponseToHttpResponse(response))
            } ~
              delete {
                var response: RestResponse = UnprocessableEntityRestResponse(Map("message" ->
                  createMessage("rest.services.service.cannot.delete.due.to.streams", serviceName)))
                val streams = getRelatedStreams(serviceName)
                if (streams.isEmpty) {
                  response = UnprocessableEntityRestResponse(Map("message" ->
                    createMessage("rest.services.service.cannot.delete.due.to.instances", serviceName)))
                  val instances = getRelatedInstances(serviceName)

                  if (instances.isEmpty) {
                    val service = serviceDAO.get(serviceName)
                    service match {
                      case Some(x) =>
                        x.destroy()
                        serviceDAO.delete(serviceName)
                        response = OkRestResponse(Map("message" ->
                          createMessage("rest.services.service.deleted", serviceName)))
                      case None =>
                        response = NotFoundRestResponse(Map("message" ->
                          createMessage("rest.services.service.notfound", serviceName)))
                    }
                  }
                }

                complete(restResponseToHttpResponse(response))
              }
          } ~
            pathPrefix("related") {
              pathEndOrSingleSlash {
                get {
                  val service = serviceDAO.get(serviceName)
                  var response: RestResponse = NotFoundRestResponse(
                    Map("message" -> createMessage("rest.services.service.notfound", serviceName)))

                  service match {
                    case Some(x) =>
                      response = OkRestResponse(Map("streams" -> getRelatedStreams(serviceName), "instances" -> getRelatedInstances(serviceName)))
                    case None =>
                  }

                  complete(restResponseToHttpResponse(response))
                }
              }
            }
        }
    }
  }

  private def getRelatedStreams(serviceName: String) = {
    streamDAO.getAll.filter(
      s => s.service.name.equals(serviceName) || usedInGenerator(s, serviceName)
    ).map(_.name)
  }

  private def usedInGenerator(stream: SjStream, serviceName: String) = {
    if (stream.streamType == StreamLiterals.tstreamType) {
      val tstream = stream.asInstanceOf[TStreamSjStream]

      tstream.generator.generatorType != GeneratorLiterals.localType && tstream.generator.service.name == serviceName
    } else false
  }

  private def getRelatedInstances(serviceName: String) = {
    instanceDAO.getAll.filter(
      s => s.coordinationService.name.equals(serviceName)
    ).map(_.name)
  }
}
