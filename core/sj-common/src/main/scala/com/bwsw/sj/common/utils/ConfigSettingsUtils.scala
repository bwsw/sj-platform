package com.bwsw.sj.common.utils


import ConfigLiterals._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository

object ConfigSettingsUtils {

  private val configService = ConnectionRepository.getConfigService

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
    getStringConfigSetting(ConfigLiterals.systemDomain + "." + getStringConfigSetting(frameworkTag))
  }

  def getTransactionGeneratorJarName() = {
    getStringConfigSetting(ConfigLiterals.systemDomain + "." + getStringConfigSetting(transactionGeneratorTag))
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