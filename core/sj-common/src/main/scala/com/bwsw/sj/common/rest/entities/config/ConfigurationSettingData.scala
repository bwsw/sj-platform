package com.bwsw.sj.common.rest.entities.config

import com.bwsw.sj.common.DAL.model.ConfigurationSetting

case class ConfigurationSettingData(name: String, value: String) {
  def asModelConfigurationSetting(domain: String) = {
    val configurationSetting = new ConfigurationSetting(
      domain + "." + this.name,
      this.value,
      domain
    )

    configurationSetting
  }
}
