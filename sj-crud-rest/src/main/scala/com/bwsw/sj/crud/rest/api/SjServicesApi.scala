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
        } ~
        get {
          val services = serviceDAO.getAll
          var msg = ""
          if (services.nonEmpty) {
            msg =  serializer.serialize(services.map(s => serviceToServiceData(s)))
          } else {
            msg = serializer.serialize(s"No services found")
          }
          complete(HttpResponse(200, entity=HttpEntity(`application/json`, msg)))

        }
      } ~
      pathPrefix(Segment) { (serviceName: String) =>
        pathEndOrSingleSlash {
          val service = serviceDAO.get(serviceName)
          var msg = ""
          if (service != null) {
            msg = serializer.serialize(serviceToServiceData(service))
          } else {
            msg = serializer.serialize(s"Service '$serviceName' not found")
          }
          complete(HttpResponse(200, entity=HttpEntity(`application/json`, msg)))
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

  /**
    * Convert service entity to service data entity
    *
    * @param service - service entity
    * @return - service data entity
    */
  def serviceToServiceData(service: Service) = {
    var serviceData: ServiceData = null
    service match {
      case s: CassandraService =>
        serviceData = new CassDBServiceData
        serviceData.name = s.name
        serviceData.description = s.description
        serviceData.asInstanceOf[CassDBServiceData].provider = s.provider.name
        serviceData.asInstanceOf[CassDBServiceData].keyspace = s.keyspace
      case s: ESService =>
        serviceData = new EsIndServiceData
        serviceData.name = s.name
        serviceData.description = s.description
        serviceData.asInstanceOf[EsIndServiceData].provider = s.provider.name
        serviceData.asInstanceOf[EsIndServiceData].index = s.index
      case s: KafkaService =>
        serviceData = new KfkQServiceData
        serviceData.name = s.name
        serviceData.description = s.description
        serviceData.asInstanceOf[KfkQServiceData].provider = s.provider.name
      case s: TStreamService =>
        serviceData = new TstrQServiceData
        serviceData.name = s.name
        serviceData.description = s.description
        serviceData.asInstanceOf[TstrQServiceData].metadataProvider = s.metadataProvider.name
        serviceData.asInstanceOf[TstrQServiceData].metadataNamespace = s.metadataNamespace
        serviceData.asInstanceOf[TstrQServiceData].dataProvider = s.dataProvider.name
        serviceData.asInstanceOf[TstrQServiceData].dataNamespace = s.dataNamespace
        serviceData.asInstanceOf[TstrQServiceData].lockProvider = s.lockProvider.name
        serviceData.asInstanceOf[TstrQServiceData].lockNamespace = s.lockNamespace
      case s: ZKService =>
        serviceData = new ZKCoordServiceData
        serviceData.name = s.name
        serviceData.description = s.description
        serviceData.asInstanceOf[ZKCoordServiceData].namespace = s.namespace
        serviceData.asInstanceOf[ZKCoordServiceData].provider = s.provider.name
      case s: RedisService =>
        serviceData = new RDSCoordServiceData
        serviceData.name = s.name
        serviceData.description = s.description
        serviceData.asInstanceOf[RDSCoordServiceData].namespace = s.namespace
        serviceData.asInstanceOf[RDSCoordServiceData].provider = s.provider.name
      case s: AerospikeService =>
        serviceData = new ArspkDBServiceData
        serviceData.name = s.name
        serviceData.description = s.description
        serviceData.asInstanceOf[ArspkDBServiceData].namespace = s.namespace
        serviceData.asInstanceOf[ArspkDBServiceData].provider = s.provider.name
      case _ =>
    }
    serviceData
  }
}
