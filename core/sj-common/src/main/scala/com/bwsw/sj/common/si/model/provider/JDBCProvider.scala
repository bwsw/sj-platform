package com.bwsw.sj.common.si.model.provider

import com.bwsw.sj.common.config.{ConfigLiterals, ConfigurationSettingsUtils}
import com.bwsw.sj.common.dal.model.provider.JDBCProviderDomain
import com.bwsw.sj.common.utils.JdbcLiterals
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

class JDBCProvider(name: String,
                   login: String,
                   password: String,
                   hosts: Array[String],
                   val driver: String,
                   description: String,
                   providerType: String)
                  (implicit injector: Injector)
  extends Provider(name, login, password, providerType, hosts, description) {

  override def to(): JDBCProviderDomain = {
    val provider =
      new JDBCProviderDomain(
        name = this.name,
        description = this.description,
        hosts = this.hosts,
        login = this.login,
        password = this.password,
        driver = this.driver
      )

    provider
  }

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
              if (!connectionRepository.getFileStorage.exists(driverFileName))
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
}
