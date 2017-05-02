package com.bwsw.sj.common.config

import java.io.File
import java.net.URL

import com.bwsw.sj.common.DAL.model.ConfigurationSetting
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigurationSettingsUtils._
import org.apache.commons.io.FileUtils

object TempHelperForConfigSetup extends App {

  //ConnectionRepository.getFileStorage.put(new File("GeoIPASNum.dat"), "GeoIPASNum.dat")
  //ConnectionRepository.getFileStorage.put(new File("GeoIPASNumv6.dat"), "GeoIPASNumv6.dat")

  val configService = ConnectionRepository.getConfigService

  configService.save(new ConfigurationSetting(ConfigLiterals.frameworkTag, "com.bwsw.fw-1.0", ConfigLiterals.systemDomain))

  configService.save(new ConfigurationSetting(createConfigurationSettingName(ConfigLiterals.systemDomain, "regular-streaming-validator-class"),
    "com.bwsw.sj.crud.rest.validator.instance.RegularInstanceValidator", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(createConfigurationSettingName(ConfigLiterals.systemDomain, "batch-streaming-validator-class"),
    "com.bwsw.sj.crud.rest.validator.instance.BatchInstanceValidator", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(createConfigurationSettingName(ConfigLiterals.systemDomain, "output-streaming-validator-class"),
    "com.bwsw.sj.crud.rest.validator.instance.OutputInstanceValidator", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(createConfigurationSettingName(ConfigLiterals.systemDomain, "input-streaming-validator-class"),
    "com.bwsw.sj.crud.rest.validator.instance.InputInstanceValidator", ConfigLiterals.systemDomain))

  configService.save(new ConfigurationSetting(ConfigLiterals.marathonTag, "http://stream-juggler.z1.netpoint-dc.com:8080", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.marathonTimeoutTag, "60000", ConfigLiterals.systemDomain))

  configService.save(new ConfigurationSetting(ConfigLiterals.zkSessionTimeoutTag, "7000", ConfigLiterals.zookeeperDomain))

  //configService.save(new ConfigurationSetting("session.timeout.ms", "30000", ConfigConstants.kafkaDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.jdbcTimeoutTag, "6000", ConfigLiterals.jdbcDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.restTimeoutTag, "5000", ConfigLiterals.restDomain))

  configService.save(new ConfigurationSetting(ConfigLiterals.kafkaSubscriberTimeoutTag, "100", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.lowWatermark, "100", ConfigLiterals.systemDomain))

  configService.save(new ConfigurationSetting(ConfigLiterals.geoIpAsNum, "GeoIPASNum.dat", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.geoIpAsNumv6, "GeoIPASNumv6.dat", ConfigLiterals.systemDomain))

  val driverName = "mysql"
  val driverFileName = s"$driverName.jar"
  private val driver: File = new File(driverFileName)
  FileUtils.copyURLToFile(
    new URL("http://central.maven.org/maven2/mysql/mysql-connector-java/5.1.41/mysql-connector-java-5.1.41.jar"),
    driver)
  ConnectionRepository.getFileStorage.put(driver, driverFileName)
  configService.save(new ConfigurationSetting(s"${ConfigLiterals.jdbcDriver}.$driverName", driverFileName, ConfigLiterals.jdbcDomain))
  configService.save(new ConfigurationSetting(s"${ConfigLiterals.jdbcDriver}.$driverName.class", "com.mysql.jdbc.Driver", ConfigLiterals.jdbcDomain))
  configService.save(new ConfigurationSetting(s"${ConfigLiterals.jdbcDriver}.$driverName.prefix", "jdbc:mysql", ConfigLiterals.jdbcDomain))
}

object TempHelperForConfigDestroy extends App {
  ConnectionRepository.getConfigService.delete(ConfigLiterals.frameworkTag)
  ConnectionRepository.getConfigService.delete(createConfigurationSettingName(ConfigLiterals.systemDomain, "regular-streaming-validator-class"))
  ConnectionRepository.getConfigService.delete(createConfigurationSettingName(ConfigLiterals.systemDomain, "batch-streaming-validator-class"))
  ConnectionRepository.getConfigService.delete(createConfigurationSettingName(ConfigLiterals.systemDomain, "output-streaming-validator-class"))
  ConnectionRepository.getConfigService.delete(createConfigurationSettingName(ConfigLiterals.systemDomain, "input-streaming-validator-class"))
  ConnectionRepository.getConfigService.delete(ConfigLiterals.marathonTag)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.marathonTimeoutTag)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.zkSessionTimeoutTag)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.kafkaSubscriberTimeoutTag)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.lowWatermark)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.geoIpAsNum)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.geoIpAsNumv6)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.jdbcTimeoutTag)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.restTimeoutTag)

  val driverName = "mysql"
  ConnectionRepository.getConfigService.delete(s"${ConfigLiterals.jdbcDriver}.$driverName")
  ConnectionRepository.getConfigService.delete(s"${ConfigLiterals.jdbcDriver}.$driverName.class")
  ConnectionRepository.getConfigService.delete(s"${ConfigLiterals.jdbcDriver}.$driverName.prefix")

  ConnectionRepository.getFileStorage.delete(s"$driverName.jar")
}