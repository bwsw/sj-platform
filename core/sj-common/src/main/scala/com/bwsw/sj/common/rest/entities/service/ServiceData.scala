package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils
import com.bwsw.sj.common.utils.ServiceLiterals
import com.bwsw.sj.common.utils.ServiceLiterals._
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonProperty, JsonSubTypes, JsonTypeInfo}

import scala.collection.mutable.ArrayBuffer


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(Array(
  new Type(value = classOf[CassDBServiceData], name = ServiceLiterals.cassandraType),
  new Type(value = classOf[EsIndServiceData], name = ServiceLiterals.elasticsearchType),
  new Type(value = classOf[KfkQServiceData], name = ServiceLiterals.kafkaType),
  new Type(value = classOf[TstrQServiceData], name = ServiceLiterals.tstreamsType),
  new Type(value = classOf[ZKCoordServiceData], name = ServiceLiterals.tstreamsType),
  new Type(value = classOf[ArspkDBServiceData], name = ServiceLiterals.aerospikeType),
  new Type(value = classOf[JDBCServiceData], name = ServiceLiterals.jdbcType)
))
class ServiceData() extends ValidationUtils {
  @JsonProperty("type") var serviceType: String = null
  var name: String = null
  var description: String = "No description"

  def asModelService(): Service = ???

  protected def fillModelService(modelService: Service) = {
    modelService.serviceType = this.serviceType
    modelService.name = this.name
    modelService.description = this.description
  }

  def validate() = validateGeneralFields()

  protected def validateGeneralFields() = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val errors = new ArrayBuffer[String]()

    // 'serviceType field
    Option(this.serviceType) match {
      case None =>
        errors += s"'Type' is required"
      case Some(x) =>
        if (!types.contains(x)) errors += s"Unknown 'type' provided. Must be one of: ${types.mkString("[", ", ", "]")}"
    }

    // 'name' field
    Option(this.name) match {
      case None =>
        errors += s"'Name' is required"
      case Some(x) =>
        if (serviceDAO.get(x).isDefined) {
          errors += s"Service with name $x already exists"
        }

        if (!validateName(x)) {
          errors += s"Service has incorrect name: $x. " +
            s"Name of service must be contain digits, lowercase letters or hyphens. First symbol must be a letter"
        }
    }

    errors
  }
}


