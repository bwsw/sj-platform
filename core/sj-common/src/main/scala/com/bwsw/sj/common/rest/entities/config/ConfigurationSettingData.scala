package com.bwsw.sj.common.rest.entities.config

import com.bwsw.sj.common.DAL.model.ConfigurationSetting
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils
import com.bwsw.sj.common.utils.ConfigurationSettingsUtils._

import scala.collection.mutable.ArrayBuffer

case class ConfigurationSettingData(name: String, value: String) extends ValidationUtils {
  def asModelConfigurationSetting(domain: String) = {
    val configurationSetting = new ConfigurationSetting(
      createConfigurationSettingName(domain, this.name),
      this.value,
      domain
    )

    configurationSetting
  }

  def validate() = {
    val configService = ConnectionRepository.getConfigService
    val errors = new ArrayBuffer[String]()

    // 'name' field
    Option(this.name) match {
      case None =>
        errors += s"'Name' is required"
      case Some(x) =>
        if (configService.get(x).isDefined) {
          errors += s"Configuration setting with name '$x' already exists"
        }

        if (!validateName(x)) {
          errors += s"Configuration setting has incorrect name: $x. " +
            s"Name of configuration setting must be contain digits, lowercase letters or hyphens. First symbol must be a letter"
        }
    }

    // 'value' field
    Option(this.value) match {
      case None =>
        errors += s"'Value' is required"
      case Some(x) =>
        if (x.isEmpty)
          errors += s"'Value' can not be empty"
    }

    errors
  }
}
