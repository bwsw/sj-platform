package com.bwsw.sj.common.utils

object ConfigurationSettingsUtils {
  def createConfigurationSettingName(domain: String, name: String) = {
    domain + "." + name
  }

  def clearConfigurationSettingName(domain: String, name: String) = {
    name.replace(domain + ".", "")
  }
}
