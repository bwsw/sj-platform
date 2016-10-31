package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.JDBCService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceLiterals

import scala.collection.mutable.ArrayBuffer

class JDBCServiceData() extends ServiceData() {
  serviceType = ServiceLiterals.jdbcType
  var provider: String = null
  var driver: String = null
  var database: String = null
  val validDrivers: List[String] = List("postgresql", "oracle", "mysql")

  override def asModelService() = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new JDBCService()
    super.fillModelService(modelService)
    modelService.provider = providerDAO.get(this.provider).get
    modelService.driver = this.driver
    modelService.database = this.database
    modelService
  }

  override def validate() = {
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider(this.provider, this.serviceType)
    // todo

    // 'driver' field
    Option(this.driver) match {
      case None =>
        errors += "'Driver' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'Driver' is required"
        }
        else {
          if (!validDrivers.contains(x)) {
            errors += s"'Driver' can be one of: $validDrivers"
          }
        }
    }

    // 'database' field
    Option(this.database) match {
      case None =>
        errors += "'Database' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'Database' is required"
        }
    }

    errors
  }
}
