package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.common.exceptions._
import com.bwsw.sj.common.rest.entities._
import com.bwsw.sj.common.rest.entities.service.ServiceData
import com.bwsw.sj.common.utils.ServiceLiterals
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

import scala.collection.mutable.ArrayBuffer

trait SjServicesApi extends Directives with SjCrudValidator {

  val servicesApi = {
    pathPrefix("services") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          var response: RestResponse = null
          val errors = new ArrayBuffer[String]
          try {
            val protocolService = serializer.deserialize[ServiceData](getEntityFromContext(ctx))
            errors ++= protocolService.validate()

            if (errors.isEmpty) {
              val service = protocolService.asModelService()
              service.prepare()
              serviceDAO.save(service)
              response = CreatedRestResponse(MessageResponseEntity(createMessage("rest.services.service.created", service.name)))
            }
          } catch {
            case e: JsonDeserializationException =>
              errors += JsonDeserializationErrorMessageCreator(e)
          }

          if (errors.nonEmpty) {
            response = BadRequestRestResponse(MessageResponseEntity(
              createMessage("rest.services.service.cannot.create", errors.mkString(";"))
            ))
          }

          ctx.complete(restResponseToHttpResponse(response))
        } ~
          get {
            val services = serviceDAO.getAll
            val response = OkRestResponse(ServicesResponseEntity())
            if (services.nonEmpty) {
              response.entity = ServicesResponseEntity(services.map(_.asProtocolService()))
            }

            complete(restResponseToHttpResponse(response))
          }
      } ~
        pathPrefix("_types") {
          pathEndOrSingleSlash {
            get {
              val response = OkRestResponse(TypesResponseEntity(ServiceLiterals.types))

              complete(restResponseToHttpResponse(response))
            }
          }
        } ~
        pathPrefix(Segment) { (serviceName: String) =>
          pathEndOrSingleSlash {
            get {
              val service = serviceDAO.get(serviceName)
              var response: RestResponse = NotFoundRestResponse(MessageResponseEntity(
                createMessage("rest.services.service.notfound", serviceName)))
              service match {
                case Some(x) =>
                  response = OkRestResponse(ServiceResponseEntity(x.asProtocolService()))
                case None =>
              }

              complete(restResponseToHttpResponse(response))
            } ~
              delete {
                var response: RestResponse = UnprocessableEntityRestResponse(MessageResponseEntity(
                  createMessage("rest.services.service.cannot.delete.due.to.streams", serviceName)))
                val streams = getRelatedStreams(serviceName)
                if (streams.isEmpty) {
                  response = UnprocessableEntityRestResponse(MessageResponseEntity(
                    createMessage("rest.services.service.cannot.delete.due.to.instances", serviceName)))
                  val instances = getRelatedInstances(serviceName)

                  if (instances.isEmpty) {
                    val service = serviceDAO.get(serviceName)
                    service match {
                      case Some(x) =>
                        x.destroy()
                        serviceDAO.delete(serviceName)
                        response = OkRestResponse(MessageResponseEntity(
                          createMessage("rest.services.service.deleted", serviceName)))
                      case None =>
                        response = NotFoundRestResponse(MessageResponseEntity(
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
                    MessageResponseEntity(createMessage("rest.services.service.notfound", serviceName)))

                  service match {
                    case Some(x) =>
                      response = OkRestResponse(
                        RelatedToServiceResponseEntity(getRelatedStreams(serviceName), getRelatedInstances(serviceName)))
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
      s => s.service.name.equals(serviceName)
    ).map(_.name)
  }

  private def getRelatedInstances(serviceName: String) = {
    instanceDAO.getAll.filter(
      s => s.coordinationService.name.equals(serviceName)
    ).map(_.name)
  }
}
