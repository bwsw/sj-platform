package com.bwsw.sj.common.rest.model.service

import com.bwsw.common.jdbc.JdbcClientBuilder
import com.bwsw.sj.common.dal.model.provider.JDBCProvider
import com.bwsw.sj.common.dal.model.service.JDBCService
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceLiterals

import scala.collection.mutable.ArrayBuffer
import com.bwsw.sj.common.rest.utils.ValidationUtils._
import com.bwsw.sj.common.utils.MessageResourceUtils._

import scala.util.{Failure, Success, Try}

class JDBCServiceData() extends ServiceData() {
  serviceType = ServiceLiterals.jdbcType
  var provider: String = null
  var database: String = null

  override def asModelService(): JDBCService = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new JDBCService(
      this.name,
      this.description,
      providerDAO.get(this.provider).get.asInstanceOf[JDBCProvider],
      this.database
    )

    modelService
  }

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider(this.provider, this.serviceType)

    // 'name' field
    val charSequence: CharSequence = "-"
    if (Option(this.name).isDefined && this.name.contains(charSequence))
      errors += createMessage("jdbc.error.service.name.contains", "-")

    // 'database' field
    Option(this.database) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Database")
      case Some(dbName) =>
        if (dbName.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Database")
        } else if (errors.isEmpty) { //provider should exist in the following test
          val providerDAO = ConnectionRepository.getProviderService
          var database_exists: Boolean = false
          val provider = providerDAO.get(this.provider).get.asInstanceOf[JDBCProvider]
          Try {
            val client = JdbcClientBuilder.
              setDriver(provider.driver).
              setDatabase(dbName).
              setHosts(provider.hosts).
              setUsername(provider.login).
              setPassword(provider.password).
              build()

            client.start()
            database_exists = true
            client.close()
          } match {
            case Success(_) =>
            case Failure(e: RuntimeException) =>
              errors += createMessage("jdbc.error.cannot.create.client", e.getMessage)
            case Failure(e) =>
              e.printStackTrace()
          }

          if (!database_exists) {
            errors += createMessage("entity.error.doesnot.exist", "Database", dbName)
          }
        }
    }

    errors
  }
}
