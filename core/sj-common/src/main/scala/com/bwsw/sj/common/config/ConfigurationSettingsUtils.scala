package com.bwsw.sj.common.config

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigLiterals._

object ConfigurationSettingsUtils {
  private val configService = ConnectionRepository.getConfigService

  def createConfigurationSettingName(domain: String, name: String) = {
    domain + "." + name
  }

  def clearConfigurationSettingName(domain: String, name: String) = {
    name.replace(domain + ".", "")
  }

  def getClientRetryPeriod() = {
    getIntConfigSetting(tgClientRetryPeriodTag)
  }

  def getServerRetryPeriod() = {
    getIntConfigSetting(tgServerRetryPeriodTag)
  }

  def getRetryCount() = {
    getIntConfigSetting(tgRetryCountTag)
  }

  def getGeoIpAsNumFileName() = {
    getStringConfigSetting(geoIpAsNum)
  }

  def getGeoIpAsNumv6FileName() = {
    getStringConfigSetting(geoIpAsNumv6)
  }

  def getKafkaSubscriberTimeout() = {
    getIntConfigSetting(kafkaSubscriberTimeoutTag)
  }

  def getZkSessionTimeout() = {
    getIntConfigSetting(zkSessionTimeoutTag)
  }

  def getFrameworkJarName() = {
    getStringConfigSetting(createConfigurationSettingName(ConfigLiterals.systemDomain, getStringConfigSetting(frameworkTag)))
  }

  def getTransactionGeneratorJarName() = {
    getStringConfigSetting(createConfigurationSettingName(ConfigLiterals.systemDomain, getStringConfigSetting(transactionGeneratorTag)))
  }

  def getCrudRestHost() = {
    getStringConfigSetting(hostOfCrudRestTag)
  }

  def getCrudRestPort() = {
    getIntConfigSetting(portOfCrudRestTag)
  }

  def getMarathonConnect() = {
    getStringConfigSetting(marathonTag)
  }

  def getMarathonTimeout() = {
    getIntConfigSetting(marathonTimeoutTag)
  }

  private def getIntConfigSetting(name: String) = {
    val maybeSetting = configService.get(name)
    if (maybeSetting.isDefined)
      maybeSetting.get.value.toInt
    else
      throw new NoSuchFieldException(s"Config setting is named '$name' has not found")
  }

  private def getStringConfigSetting(name: String) = {
    val maybeSetting = configService.get(name)
    if (maybeSetting.isDefined)
      maybeSetting.get.value
    else
      throw new NoSuchFieldException(s"Config setting is named '$name' has not found")
  }
}
