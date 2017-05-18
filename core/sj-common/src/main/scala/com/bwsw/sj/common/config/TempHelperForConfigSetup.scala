package com.bwsw.sj.common.config

import java.io.File
import java.net.URL

import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import com.bwsw.sj.common.utils.RestLiterals
import org.apache.commons.io.FileUtils

object TempHelperForConfigSetup extends App {

  //ConnectionRepository.getFileStorage.put(new File("GeoIPASNum.dat"), "GeoIPASNum.dat")
  //ConnectionRepository.getFileStorage.put(new File("GeoIPASNumv6.dat"), "GeoIPASNumv6.dat")

  val configService: GenericMongoRepository[ConfigurationSettingDomain] = ConnectionRepository.getConfigRepository

  configService.save(ConfigurationSettingDomain(ConfigLiterals.frameworkTag, "com.bwsw.fw-1.0", ConfigLiterals.systemDomain))

  configService.save(ConfigurationSettingDomain(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "regular-streaming-validator-class"),
    "com.bwsw.sj.crud.rest.validator.instance.RegularInstanceValidator", ConfigLiterals.systemDomain))
  configService.save(ConfigurationSettingDomain(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "batch-streaming-validator-class"),
    "com.bwsw.sj.crud.rest.validator.instance.BatchInstanceValidator", ConfigLiterals.systemDomain))
  configService.save(ConfigurationSettingDomain(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "output-streaming-validator-class"),
    "com.bwsw.sj.crud.rest.validator.instance.OutputInstanceValidator", ConfigLiterals.systemDomain))
  configService.save(ConfigurationSettingDomain(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "input-streaming-validator-class"),
    "com.bwsw.sj.crud.rest.validator.instance.InputInstanceValidator", ConfigLiterals.systemDomain))

  configService.save(ConfigurationSettingDomain(ConfigLiterals.marathonTag, "http://stream-juggler.z1.netpoint-dc.com:8080", ConfigLiterals.systemDomain))
  configService.save(ConfigurationSettingDomain(ConfigLiterals.marathonTimeoutTag, "60000", ConfigLiterals.systemDomain))

  configService.save(ConfigurationSettingDomain(ConfigLiterals.zkSessionTimeoutTag, "7000", ConfigLiterals.zookeeperDomain))

  //configService.save(new ConfigurationSetting("session.timeout.ms", "30000", ConfigConstants.kafkaDomain))
  configService.save(ConfigurationSettingDomain(ConfigLiterals.jdbcTimeoutTag, "6000", ConfigLiterals.jdbcDomain))
  configService.save(ConfigurationSettingDomain(ConfigLiterals.restTimeoutTag, "5000", ConfigLiterals.restDomain))

  configService.save(ConfigurationSettingDomain(ConfigLiterals.kafkaSubscriberTimeoutTag, "100", ConfigLiterals.systemDomain))
  configService.save(ConfigurationSettingDomain(ConfigLiterals.lowWatermark, "100", ConfigLiterals.systemDomain))

  configService.save(ConfigurationSettingDomain(ConfigLiterals.geoIpAsNum, "GeoIPASNum.dat", ConfigLiterals.systemDomain))
  configService.save(ConfigurationSettingDomain(ConfigLiterals.geoIpAsNumv6, "GeoIPASNumv6.dat", ConfigLiterals.systemDomain))

  val driverName: String = "mysql"
  val driverFileName: String = s"$driverName.jar"
  private val driver: File = new File(driverFileName)
  FileUtils.copyURLToFile(
    new URL("http://central.maven.org/maven2/mysql/mysql-connector-java/5.1.41/mysql-connector-java-5.1.41.jar"),
    driver)
  ConnectionRepository.getFileStorage.put(driver, driverFileName, Map("description" -> RestLiterals.defaultDescription), FileMetadata.customFileType)
  configService.save(ConfigurationSettingDomain(s"${ConfigLiterals.jdbcDriver}.$driverName", driverFileName, ConfigLiterals.jdbcDomain))
  configService.save(ConfigurationSettingDomain(s"${ConfigLiterals.jdbcDriver}.$driverName.class", "com.mysql.jdbc.Driver", ConfigLiterals.jdbcDomain))
  configService.save(ConfigurationSettingDomain(s"${ConfigLiterals.jdbcDriver}.$driverName.prefix", "jdbc:mysql", ConfigLiterals.jdbcDomain))
}

object TempHelperForConfigDestroy extends App {
  ConnectionRepository.getConfigRepository.delete(ConfigLiterals.frameworkTag)
  ConnectionRepository.getConfigRepository.delete(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "regular-streaming-validator-class"))
  ConnectionRepository.getConfigRepository.delete(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "batch-streaming-validator-class"))
  ConnectionRepository.getConfigRepository.delete(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "output-streaming-validator-class"))
  ConnectionRepository.getConfigRepository.delete(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "input-streaming-validator-class"))
  ConnectionRepository.getConfigRepository.delete(ConfigLiterals.marathonTag)
  ConnectionRepository.getConfigRepository.delete(ConfigLiterals.marathonTimeoutTag)
  ConnectionRepository.getConfigRepository.delete(ConfigLiterals.zkSessionTimeoutTag)
  ConnectionRepository.getConfigRepository.delete(ConfigLiterals.kafkaSubscriberTimeoutTag)
  ConnectionRepository.getConfigRepository.delete(ConfigLiterals.lowWatermark)
  ConnectionRepository.getConfigRepository.delete(ConfigLiterals.geoIpAsNum)
  ConnectionRepository.getConfigRepository.delete(ConfigLiterals.geoIpAsNumv6)
  ConnectionRepository.getConfigRepository.delete(ConfigLiterals.jdbcTimeoutTag)
  ConnectionRepository.getConfigRepository.delete(ConfigLiterals.restTimeoutTag)

  val driverName: String = "mysql"
  ConnectionRepository.getConfigRepository.delete(s"${ConfigLiterals.jdbcDriver}.$driverName")
  ConnectionRepository.getConfigRepository.delete(s"${ConfigLiterals.jdbcDriver}.$driverName.class")
  ConnectionRepository.getConfigRepository.delete(s"${ConfigLiterals.jdbcDriver}.$driverName.prefix")

  ConnectionRepository.getFileStorage.delete(s"$driverName.jar")
}