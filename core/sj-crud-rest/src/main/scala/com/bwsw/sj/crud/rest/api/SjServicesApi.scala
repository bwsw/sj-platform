package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.common.exceptions.BadRecordWithKey
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.crud.rest.entities._
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
            ServiceUtil.prepareService(service) //todo or when running instance?
            serviceDAO.save(service)
            val response = ProtocolResponse(200, Map("message" -> s"Service '${service.name}' is created"))
            ctx.complete(HttpEntity(`application/json`, serializer.serialize(response)))
          } else {
            throw new BadRecordWithKey(
              s"Cannot create service. Errors: ${errors.mkString("\n")}",
              s"${data.name}"
            )
          }
        } ~
        get {
          val services = serviceDAO.getAll
          var response: ProtocolResponse = null
          if (services.nonEmpty) {
            val entity = Map("services" -> services.map(s => serviceToServiceData(s)))
            response = ProtocolResponse(200, entity)
          } else {
            response = ProtocolResponse(200, Map("message" -> "No services found"))
          }
          complete(HttpEntity(`application/json`, serializer.serialize(response)))

        }
      } ~
      pathPrefix(Segment) { (serviceName: String) =>
        pathEndOrSingleSlash {
          get {
            val service = serviceDAO.get(serviceName)
            var response: ProtocolResponse = null
            if (service != null) {
              val entity = Map("services" -> serviceToServiceData(service))
              response = ProtocolResponse(200, entity)
            } else {
              response = ProtocolResponse(200, Map("message" -> s"Service '$serviceName' not found"))
            }
            complete(HttpEntity(`application/json`, serializer.serialize(response)))
          } ~
          delete {
            val streams = streamDAO.getAll.filter(s => s.service.name.equals(serviceName))
            if (streams.isEmpty) {
              val service = serviceDAO.get(serviceName)
              var response: ProtocolResponse = null
              if (service != null) {
                serviceDAO.delete(serviceName)
                response = ProtocolResponse(200, Map("message" -> s"Service '$serviceName' has been deleted"))
              } else {
                response = ProtocolResponse(200, Map("message" -> s"Service '$serviceName' not found"))
              }
              complete(HttpEntity(`application/json`, serializer.serialize(response)))
            } else {
              throw new BadRecordWithKey(s"Cannot delete service $serviceName. Service usage in streams", serviceName)
            }
          }
        }
      }
    }
  }
}
