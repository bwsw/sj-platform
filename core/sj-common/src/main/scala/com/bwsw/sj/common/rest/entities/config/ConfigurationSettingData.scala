package com.bwsw.sj.common.rest.entities.config

import com.bwsw.sj.common.DAL.model.ConfigurationSetting
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils
import com.bwsw.sj.common.utils.ConfigLiterals
import com.bwsw.sj.common.utils.ConfigurationSettingsUtils._
import com.bwsw.tstreams.env.TSF_Dictionary

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

  def validate(domain: String) = {
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

        if (!validateConfigSettingName(x)) {
          errors += s"Configuration setting has incorrect name: $x. " +
            s"Name of configuration setting must contain lowercase letters, hyphens or periods. First symbol must be a letter"
        }

        if (domain == ConfigLiterals.tstreamsDomain && !validateTstreamProperty()) {
          errors += s"Configuration setting has incorrect name: $x. " +
            s"T-streams domain configuration setting must be only for consumer or producer"
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

  private def validateTstreamProperty(): Boolean = {
    this.name.contains("producer") || this.name.contains("consumer") || this.name == TSF_Dictionary.Producer.Transaction.DISTRIBUTION_POLICY
  }
}
