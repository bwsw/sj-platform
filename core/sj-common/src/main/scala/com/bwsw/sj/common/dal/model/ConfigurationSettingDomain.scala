package com.bwsw.sj.common.dal.model

import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.IdField
import com.bwsw.sj.common.config.ConfigurationSettingsUtils._
import com.bwsw.sj.common.rest.model.config.ConfigurationSettingApi
import org.mongodb.morphia.annotations.Entity

/**
  * Entity for one element from configuration settings.
  * Configuration settings is a whole collection in mongo,
  * collection element is one configuration setting.
  */
@Entity("config")
class ConfigurationSettingDomain(@IdField val name: String, val value: String, val domain: String) {

  def asProtocolConfigurationSetting(): ConfigurationSettingApi = {
    val configurationSettingData =
      ConfigurationSettingApi(
        name = clearConfigurationSettingName(domain, name),
        value = this.value,
        domain = this.domain
      )

    configurationSettingData
  }
}
