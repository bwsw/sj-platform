package com.bwsw.sj.common.DAL.model

import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.bwsw.sj.common.rest.entities.config.ConfigurationSettingData
import ConfigurationSettingsUtils._
import org.mongodb.morphia.annotations.{Id, Entity}

/**
 * Entity for one element from configuration settings.
 * Configuration settings is a whole collection in mongo,
 * collection element is one configuration setting.
 */
@Entity("config")
class ConfigurationSetting {
  @Id var name: String = null
  var value: String = null
  var domain: String = null

  def this(name: String, value: String, domain: String) = {
    this()
    this.name = name
    this.value = value
    this.domain = domain
  }
  
  def asProtocolConfigurationSetting() = {
    val configurationSettingData = new ConfigurationSettingData(
      clearConfigurationSettingName(domain, name),
      this.value
    )
    
    configurationSettingData
  }
}
