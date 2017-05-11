package com.bwsw.sj.common.si.model.service

import com.bwsw.common.jdbc.JdbcClientBuilder
import com.bwsw.sj.common.dal.model.provider.JDBCProviderDomain
import com.bwsw.sj.common.dal.model.service.JDBCServiceDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils.validateProvider
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage

import scala.collection.mutable.ArrayBuffer

class JDBCService(name: String,
                  val database: String,
                  val provider: String,
                  description: String,
                  serviceType: String)
  extends Service(serviceType, name, description) {

  override def to(): JDBCServiceDomain = {
    val providerRepository = ConnectionRepository.getProviderRepository
    val provider = providerRepository.get(this.provider).get.asInstanceOf[JDBCProviderDomain]

    val modelService =
      new JDBCServiceDomain(
        name = this.name,
        description = this.description,
        provider = provider,
        database = this.database
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
        val providerRepository = ConnectionRepository.getProviderRepository
          var database_exists: Boolean = false
          val provider = providerRepository.get(this.provider).get.asInstanceOf[JDBCProviderDomain]
          try {
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
          } catch {
            case e: Exception =>
              e.printStackTrace()
            case e: RuntimeException =>
              errors += createMessage("jdbc.error.cannot.create.client", e.getMessage)
          }

          if (!database_exists) {
            errors += createMessage("entity.error.doesnot.exist", "Database", dbName)
          }
        }
    }

    errors
  }
}
