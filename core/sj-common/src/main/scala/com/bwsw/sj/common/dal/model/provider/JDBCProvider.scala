package com.bwsw.sj.common.dal.model.provider

import java.sql.SQLException

import com.bwsw.common.jdbc.JdbcClientBuilder
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.config.{ConfigLiterals, ConfigurationSettingsUtils}
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.{JdbcLiterals, ProviderLiterals}

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

class JDBCProvider(override val name: String,
                   override val description: String,
                   override val hosts: Array[String],
                   override val login: String,
                   override val password: String,
                   val driver: String,
                   override val providerType: String = ProviderLiterals.jdbcType)
  extends Provider(name, description, hosts, login, password, providerType) {

  override def validate(): ArrayBuffer[String] = {
    val errors = super.validate()

    // 'driver' field
    Option(this.driver) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Driver")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Driver")
        }
        else {
          Try(ConfigurationSettingsUtils.getJdbcDriverFileName(x)) match {
            case Success(driverFileName) =>
              if (!ConnectionRepository.getFileStorage.exists(driverFileName))
                errors += createMessage("entity.error.file.required", driverFileName)
            case Failure(_: NoSuchFieldException) =>
              errors += createMessage("entity.error.config.required", s"${ConfigLiterals.jdbcDriver}.$x")
            case Failure(e) => throw e
          }

          Try(ConfigurationSettingsUtils.getJdbcDriverClass(x)) match {
            case Success(_) =>
            case Failure(_: NoSuchFieldException) =>
              errors += createMessage("entity.error.config.required", s"${ConfigLiterals.jdbcDriver}.$x.class")
            case Failure(e) => throw e
          }

          val prefixSettingName = s"${ConfigLiterals.jdbcDriver}.$x.prefix"
          Try(ConfigurationSettingsUtils.getJdbcDriverPrefix(x)) match {
            case Success(prefix) =>
              if (!JdbcLiterals.validPrefixes.contains(prefix))
                errors += createMessage("entity.error.jdbc.prefix.incorrect", prefix, prefixSettingName)
            case Failure(_: NoSuchFieldException) =>
              errors += createMessage("entity.error.config.required", prefixSettingName)
            case Failure(e) => throw e
          }
        }
    }

    errors
  }

  override def checkJdbcConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    Try {
      val client = JdbcClientBuilder.
        setHosts(hosts).
        setDriver(driver).
        setUsername(login).
        setPassword(password).
        build()

      client.checkConnectionToDatabase()
    } match {
      case Success(_) =>
      case Failure(ex: SQLException) =>
        ex.printStackTrace()
        errors += s"Cannot gain an access to JDBC on '$address'"
      case Failure(e) => throw e
    }

    errors
  }
}
