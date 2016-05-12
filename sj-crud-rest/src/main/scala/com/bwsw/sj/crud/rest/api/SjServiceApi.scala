package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.common.exceptions.BadRecordWithKey
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.crud.rest.entities._
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.service.ServiceValidator

/**
  * Rest-api for streams
  *
  * Created by mendelbaum_nm
  */
trait SjServiceApi extends Directives with SjCrudValidator {

  val serviceApi = {
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
          }

          val errors = validateService(data, service)

          if (errors.isEmpty) {
            val serviceName = saveService(service)
            ctx.complete(HttpEntity(
              `application/json`,
              serializer.serialize(Response(200, serviceName, s"Service '$serviceName' is created"))
            ))
          } else {
            throw new BadRecordWithKey(
              s"Cannot create service. Errors: ${errors.mkString("\n")}",
              s"${data.name}"
            )
          }
        }
      }
    }
  }

  /**
    * Validation of data for service being created and filling in the service object
    *
    * @param serviceData - stream
    * @return - errors
    */
  def validateService(serviceData: ServiceData, service: Service) = {
    // Using common validator for all service types
    val validator = new ServiceValidator
    validator.validate(serviceData, service)
  }

  /**
    * Save service of any type to db
    *
    * @param service - service entity
    * @return - name of saved entity
    */
  def saveService(service: Service) = {
    serviceDAO.save(service)
    service.name
  }
}
