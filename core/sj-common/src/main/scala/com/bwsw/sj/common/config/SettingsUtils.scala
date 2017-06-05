package com.bwsw.sj.common.config

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigLiterals._
import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import scaldi.Injectable.inject
import scaldi.Injector

class SettingsUtils(implicit val injector: Injector) {
  private val configService = inject[ConnectionRepository].getConfigRepository

  def getGeoIpAsNumFileName(): String = {
    getStringConfigSetting(geoIpAsNum)
  }

  def getGeoIpAsNumv6FileName(): String = {
    getStringConfigSetting(geoIpAsNumv6)
  }

  def getKafkaSubscriberTimeout(): Int = {
    getIntConfigSetting(kafkaSubscriberTimeoutTag)
  }

  def getZkSessionTimeout(): Int = {
    getIntConfigSetting(zkSessionTimeoutTag)
  }

  def getFrameworkJarName(): String = {
    getStringConfigSetting(
      ConfigurationSetting.createConfigurationSettingName(
        ConfigLiterals.systemDomain,
        getStringConfigSetting(frameworkTag)
      )
    )
  }

  def getCrudRestHost(): String = {
    getStringConfigSetting(hostOfCrudRestTag)
  }

  def getCrudRestPort(): Int = {
    getIntConfigSetting(portOfCrudRestTag)
  }

  def getMarathonConnect(): String = {
    getStringConfigSetting(marathonTag)
  }

  def getMarathonTimeout(): Int = {
    getIntConfigSetting(marathonTimeoutTag)
  }

  def getFrameworkBackoffSeconds(): Int = {
    getIntConfigSetting(frameworkBackoffSeconds)
  }

  def getFrameworkBackoffFactor(): Double = {
    getDoubleConfigSetting(frameworkBackoffFactor)
  }

  def getFrameworkMaxLaunchDelaySeconds(): Int = {
    getIntConfigSetting(frameworkMaxLaunchDelaySeconds)
  }

  def getLowWatermark(): Int = {
    getIntConfigSetting(lowWatermark)
  }

  def getJdbcDriverFileName(driverName: String): String = getStringConfigSetting(s"$jdbcDriver.$driverName")

  def getJdbcDriverClass(driverName: String): String = getStringConfigSetting(s"$jdbcDriver.$driverName.class")

  def getJdbcDriverPrefix(driverName: String): String = getStringConfigSetting(s"$jdbcDriver.$driverName.prefix")

  def getRestTimeout: Int = {
    getIntConfigSetting(restTimeoutTag)
  }

  private def getIntConfigSetting(name: String): Int = {
    getConfigSettings(name).toInt
  }

  private def getStringConfigSetting(name: String): String = {
    getConfigSettings(name)
  }

  private def getDoubleConfigSetting(name: String): Double = {
    getConfigSettings(name).toDouble
  }

  private def getConfigSettings(name: String): String = {
    val maybeSetting = configService.get(name)
    if (maybeSetting.isEmpty)
      throw new NoSuchFieldException(s"Config setting is named '$name' has not found")
    else
      maybeSetting.get.value
  }
}
