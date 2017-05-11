package com.bwsw.sj.common.si.model.provider

import com.bwsw.sj.common.config.{ConfigLiterals, ConfigurationSettingsUtils}
import com.bwsw.sj.common.dal.model.provider.JDBCProviderDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.JdbcLiterals
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage

import scala.collection.mutable.ArrayBuffer

class JDBCProvider(name: String,
                   login: String,
                   password: String,
                   hosts: Array[String],
                   val driver: String,
                   description: String,
                   providerType: String)
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

          val prefixSettingName = s"${ConfigLiterals.jdbcDriver}.$x.prefix"
          try {
            val prefix = ConfigurationSettingsUtils.getJdbcDriverPrefix(x)
            if (!JdbcLiterals.validPrefixes.contains(prefix))
              errors += createMessage("entity.error.jdbc.prefix.incorrect", prefix, prefixSettingName)
          } catch {
            case _: NoSuchFieldException =>
              errors += createMessage("entity.error.config.required", prefixSettingName)
          }
        }
    }

    errors
  }
}
