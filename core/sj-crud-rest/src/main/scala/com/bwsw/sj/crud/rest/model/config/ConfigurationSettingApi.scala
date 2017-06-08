package com.bwsw.sj.crud.rest.model.config

import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonSubTypes, JsonTypeInfo}

/**
  * Created by diryavkin_dn on 11.05.17.
  */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = classOf[ConfigurationSettingApi], visible = true)
@JsonSubTypes(Array(

))
class ConfigurationSettingApi(val name: String,
                              val value: String,
                              val domain: String) {
  @JsonIgnore
  def to(): ConfigurationSetting = {
    new ConfigurationSetting(name, value, domain)
  }

}

class CreateConfigurationSettingApi {
  def from(configSettings: ConfigurationSetting): ConfigurationSettingApi = {
    new ConfigurationSettingApi(
      configSettings.name,
      configSettings.value,
      configSettings.domain)
  }
}