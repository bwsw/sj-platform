package com.bwsw.sj.common.rest.model.service

import com.bwsw.sj.common.dal.model.service.Service
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils._
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.ServiceLiterals
import com.bwsw.sj.common.utils.ServiceLiterals._
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSubTypes, JsonTypeInfo}

import scala.collection.mutable.ArrayBuffer


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = classOf[ServiceData], visible = true)
@JsonSubTypes(Array(
  new Type(value = classOf[CassDBServiceData], name = ServiceLiterals.cassandraType),
  new Type(value = classOf[EsServiceData], name = ServiceLiterals.elasticsearchType),
  new Type(value = classOf[KfkQServiceData], name = ServiceLiterals.kafkaType),
  new Type(value = classOf[TstrQServiceData], name = ServiceLiterals.tstreamsType),
  new Type(value = classOf[ZKCoordServiceData], name = ServiceLiterals.zookeeperType),
  new Type(value = classOf[ArspkDBServiceData], name = ServiceLiterals.aerospikeType),
  new Type(value = classOf[JDBCServiceData], name = ServiceLiterals.jdbcType),
  new Type(value = classOf[RestServiceData], name = ServiceLiterals.restType)
))
class ServiceData() {
  @JsonProperty("type") var serviceType: String = _
  var name: String = _
  var description: String = "No description"

  @JsonIgnore
  def asModelService(): Service = ???

  @JsonIgnore
  def validate() = validateGeneralFields()

  @JsonIgnore
  protected def validateGeneralFields(): ArrayBuffer[String] = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val errors = new ArrayBuffer[String]()

    // 'serviceType field
    Option(this.serviceType) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Type")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Type")
        }
        else {
          if (!types.contains(x))
            errors += createMessage("entity.error.unknown.type.must.one.of", x, "service", types.mkString("[", ", ", "]"))
        }
    }

    // 'name' field
    Option(this.name) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Name")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Name")
        }
        else {
          if (!validateName(x)) {
            errors += createMessage("entity.error.incorrect.name", "Service", x, "service")
          }

          if (serviceDAO.get(x).isDefined) {
            errors += createMessage("entity.error.already.exists", "Service", x)
          }
        }
    }

    errors
  }
}


