package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.sj.common.rest.entities._
import com.bwsw.sj.common.rest.entities.service.ServiceData
import com.bwsw.sj.crud.rest.utils.CompletionUtils
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

trait SjServicesApi extends Directives with SjCrudValidator with CompletionUtils {

  val servicesApi = {
    pathPrefix("services") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
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
            var response: RestResponse = NotFoundRestResponse(Map("message" -> getMessage("rest.services.notfound")))
            if (services.nonEmpty) {
              val entity = Map("services" -> services.map(_.asProtocolService()))
              response = OkRestResponse(entity)
            }

            complete(restResponseToHttpResponse(response))
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
                  createMessage("rest.services.service.cannot.delete", serviceName)))
                val streams = getUsedStreams(serviceName)
                if (streams.isEmpty) {
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

                complete(restResponseToHttpResponse(response))
              }
          }
        }
    }
  }

  private def getUsedStreams(serviceName: String) = {
    streamDAO.getAll.filter(s => s.service.name.equals(serviceName))
  }
}
