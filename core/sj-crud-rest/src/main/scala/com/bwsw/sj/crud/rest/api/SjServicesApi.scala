package com.bwsw.sj.crud.rest.api

import java.text.MessageFormat

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.common.exceptions.BadRequestWithKey
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.crud.rest.entities._
import com.bwsw.sj.crud.rest.entities.service.ServiceData
import com.bwsw.sj.crud.rest.utils.ConvertUtil.serviceToServiceData
import com.bwsw.sj.crud.rest.utils.ServiceUtil
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.service.ServiceValidator

/**
 * Rest-api for streams
 *
 * Created by mendelbaum_nm
 */
trait SjServicesApi extends Directives with SjCrudValidator {

  val servicesApi = {
    pathPrefix("services") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          val data = serializer.deserialize[ServiceData](getEntityFromContext(ctx))

          var service = new Service
          data.serviceType match {
            case "CassDB" => service = new CassandraService
            case "ESInd" => service = new ESService
            case "KfkQ" => service = new KafkaService
            case "TstrQ" => service = new TStreamService
            case "ZKCoord" => service = new ZKService
            case "RDSCoord" => service = new RedisService
            case "ArspkDB" => service = new AerospikeService
            case "JDBC" => service = new JDBCService
          }

          val errors = ServiceValidator.validate(data, service)

          if (errors.isEmpty) {
            ServiceUtil.prepareService(service)
            serviceDAO.save(service)
            val response = OkRestResponse(Map("message" -> MessageFormat.format(
              messages.getString("rest.services.service.created"),
              service.name
            )))
            ctx.complete(HttpEntity(`application/json`, serializer.serialize(response)))
          } else {
            throw new BadRequestWithKey(
              MessageFormat.format(
                messages.getString("rest.services.service.cannot.create"),
                errors.mkString("\n")),
              s"${data.name}"
            )
          }
        } ~
          get {
            val services = serviceDAO.getAll
            var response: RestResponse = null
            if (services.nonEmpty) {
              val entity = Map("services" -> services.map(s => serviceToServiceData(s)))
              response = OkRestResponse(entity)
            } else {
              response = OkRestResponse(Map("message" -> messages.getString("rest.services.notfound")))
            }
            complete(HttpEntity(`application/json`, serializer.serialize(response)))

          }
      } ~
        pathPrefix(Segment) { (serviceName: String) =>
          pathEndOrSingleSlash {
            get {
              val service = serviceDAO.get(serviceName)
              var response: RestResponse = null
              service match {
                case Some(x) =>
                  val entity = Map("services" -> serviceToServiceData(x))
                  response = OkRestResponse(entity)
                case None =>
                  response = OkRestResponse(Map("message" -> MessageFormat.format(
                    messages.getString("rest.services.service.notfound"),
                    serviceName
                  )))
              }
              complete(HttpEntity(`application/json`, serializer.serialize(response)))
            } ~
              delete {
                val streams = streamDAO.getAll.filter(s => s.service.name.equals(serviceName))
                if (streams.isEmpty) {
                  val service = serviceDAO.get(serviceName)
                  var response: RestResponse = null
                  service match {
                    case Some(x) =>
                      ServiceUtil.deleteService(x)
                      serviceDAO.delete(serviceName)
                      response = OkRestResponse(Map("message" -> MessageFormat.format(
                        messages.getString("rest.services.service.deleted"),
                        serviceName
                      )))
                    case None =>
                      response = OkRestResponse(Map("message" -> MessageFormat.format(
                        messages.getString("rest.services.service.notfound"),
                        serviceName
                      )))
                  }
                  complete(HttpEntity(`application/json`, serializer.serialize(response)))
                } else {
                  throw new BadRequestWithKey(MessageFormat.format(
                    messages.getString("rest.services.service.cannot.delete"),
                    serviceName), serviceName)
                }
              }
          }
        }
    }
  }
}
