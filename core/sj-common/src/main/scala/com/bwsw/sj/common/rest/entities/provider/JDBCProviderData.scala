package com.bwsw.sj.common.rest.entities.provider

import com.bwsw.sj.common.DAL.model.provider.JDBCProvider
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.{ConfigLiterals, ConfigurationSettingsUtils}
import com.bwsw.sj.common.utils.JdbcLiterals
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}

import scala.collection.mutable.ArrayBuffer


class JDBCProviderData(name: String,
                       login: String,
                       password: String,
                       @JsonProperty("type") providerType: String,
                       hosts: Array[String],
                       val driver: String,
                       description: String = "No description"
                      ) extends ProviderData(name, login, password, providerType, hosts, description) {

  @JsonIgnore
  override def asModelProvider(): JDBCProvider = {
    val provider = new JDBCProvider(
      this.name,
      this.description,
      this.hosts,
      this.login,
      this.password,
      this.providerType,
      this.driver
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
