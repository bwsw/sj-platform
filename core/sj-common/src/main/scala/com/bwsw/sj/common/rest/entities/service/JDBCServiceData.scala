package com.bwsw.sj.common.rest.entities.service

import com.bwsw.common.jdbc.JdbcClientBuilder
import com.bwsw.sj.common.DAL.model.JDBCService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.{ConfigLiterals, ConfigurationSettingsUtils}
import com.bwsw.sj.common.utils.ServiceLiterals

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

    // 'name' field
    val charSequence: CharSequence = "-"
    if (this.name.contains(charSequence))
      errors += createMessage("jdbc.error.service.name.contains", "-")

    // 'driver' field
    Option(this.driver) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Driver")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Driver")
        }
        else {
          try {
            val driverFileName = ConfigurationSettingsUtils.getJdbcDriverFileName(x)
            if (!ConnectionRepository.getFileStorage.exists(driverFileName))
              errors += createMessage("entity.error.file.required", driverFileName)
          } catch {
            case _: NoSuchFieldException =>
              errors += createMessage("entity.error.config.required", s"${ConfigLiterals.jdbcDriver}.$x")
          }

          try {
            ConfigurationSettingsUtils.getJdbcDriverClass(x)
          } catch {
            case _: NoSuchFieldException =>
              errors += createMessage("entity.error.config.required", s"${ConfigLiterals.jdbcDriver}.$x.class")
          }

          try {
            ConfigurationSettingsUtils.getJdbcDriverPrefix(x)
          } catch {
            case _: NoSuchFieldException =>
              errors += createMessage("entity.error.config.required", s"${ConfigLiterals.jdbcDriver}.$x.prefix")
          }
        }
    }

    // 'database' field
    Option(this.database) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Database")
      case Some(dbName) =>
        if (dbName.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Database")
        } else {
          val providerDAO = ConnectionRepository.getProviderService
          var database_exists: Boolean = false
          try {
            val provider = providerDAO.get(this.provider).get
            JdbcClientBuilder.
              setDriver(this.driver).
              setDatabase(dbName).
              setHosts(provider.hosts).
              setUsername(provider.login).
              setPassword(provider.password).
              build()
            database_exists = true
          } catch {
            case e: Exception =>
            case e: RuntimeException =>
              errors += createMessage("jdbc.error.cannot.create.client", e.getMessage)
          }
          if (database_exists) {
            errors += createMessage("entity.error.doesnot.exist", "Database", dbName)
          }
        }
    }
    errors
  }
}
