package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.JDBCService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceLiterals
import com.bwsw.common.JdbcClientBuilder

import scala.collection.mutable.ArrayBuffer

class JDBCServiceData() extends ServiceData() {
  serviceType = ServiceLiterals.jdbcType
  var provider: String = null
  var driver: String = null
  var database: String = null

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

    // 'driver' field
    Option(this.driver) match {
      case None =>
        errors += "'Driver' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'Driver' is required"
        }
        else {
          if (!JdbcClientBuilder.validDrivers.contains(x)) {
            errors += s"Existing drivers: ${JdbcClientBuilder.validDrivers}"
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
      case Some(x) =>
        if (x.nonEmpty) {
          val providerDAO = ConnectionRepository.getProviderService
          val provider = providerDAO.get(this.provider).get
          var database_not_exists: Boolean = true
          try {
              JdbcClientBuilder.
              setTxnField("txn").
              setDriver(this.driver).
              setDatabase(x).
              setHosts(provider.hosts).
              setUsername(provider.login).
              setPassword(provider.password).
              build()
            database_not_exists = false
          } catch {
            case e:Exception =>
          }
          if (database_not_exists) {
            errors += s"Database \"$x\" doesn't exists."
          }
        }
    }

    errors
  }
}
