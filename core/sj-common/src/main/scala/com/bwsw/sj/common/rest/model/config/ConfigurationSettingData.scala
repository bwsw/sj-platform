package com.bwsw.sj.common.rest.model.config

import com.bwsw.sj.common._dal.model.ConfigurationSetting
import com.bwsw.sj.common._dal.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.config.ConfigurationSettingsUtils._
import com.bwsw.sj.common.rest.utils.ValidationUtils
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.tstreams.env.ConfigurationOptions
import com.fasterxml.jackson.annotation.JsonIgnore

import scala.collection.mutable.ArrayBuffer
import MessageResourceUtils._
import ValidationUtils._

case class ConfigurationSettingData(name: String, value: String, domain:String) {
  @JsonIgnore
  def asModelConfigurationSetting = {
    val configurationSetting = new ConfigurationSetting(
      createConfigurationSettingName(this.domain, this.name),
      this.value,
      this.domain
    )

    configurationSetting
  }

  @JsonIgnore
  def validate() = {
    val configService = ConnectionRepository.getConfigService
    val errors = new ArrayBuffer[String]()

    // 'name' field
    Option(this.name) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Name")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Name")
        }
        else {
          val modelConfigName = createConfigurationSettingName(this.domain, this.name)
          if (configService.get(modelConfigName).isDefined) {
            errors += createMessage("entity.error.already.exists", "Config setting", x)
          }

          if (!validateConfigSettingName(x)) {
            errors += createMessage("entity.error.incorrect.config.name", x)
          }

          if (this.domain == ConfigLiterals.tstreamsDomain && !validateTstreamProperty()) {
            errors += createMessage("entity.error.incorrect.name.tstreams.domain", "Config setting", x)
          }
        }
    }

    // 'value' field
    Option(this.value) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Value")
      case Some(x) =>
        if (x.isEmpty)
          errors += createMessage("entity.error.attribute.required", "Value")
    }

    // 'domain' field
    Option(this.domain) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Domain")
      case Some(x) =>
        if (x.isEmpty)
          errors += createMessage("entity.error.attribute.required", "Domain")
        else {
          if (!ConfigLiterals.domains.contains(x)) {
            errors += createMessage("rest.validator.attribute.unknown.value", "domain", s"$x") + ". " +
              createMessage("rest.validator.attribute.must.one_of", "Domain", ConfigLiterals.domains.mkString("[", ", ", "]"))
          }
        }
    }

    errors
  }

  @JsonIgnore
  private def validateTstreamProperty(): Boolean = {
    this.name.contains("producer") || this.name.contains("consumer") || this.name == ConfigurationOptions.Producer.Transaction.distributionPolicy
  }
}
