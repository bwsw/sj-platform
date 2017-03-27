package com.bwsw.sj.common.config

import com.bwsw.sj.common.DAL.model.ConfigurationSetting
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigurationSettingsUtils._

object TempHelperForConfigSetup extends App {

  //ConnectionRepository.getFileStorage.put(new File("GeoIPASNum.dat"), "GeoIPASNum.dat")
  //ConnectionRepository.getFileStorage.put(new File("GeoIPASNumv6.dat"), "GeoIPASNumv6.dat")

  val configService = ConnectionRepository.getConfigService

  configService.save(new ConfigurationSetting(ConfigLiterals.transactionGeneratorTag, "com.bwsw.tg-1.0", ConfigLiterals.systemDomain))

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

  configService.save(new ConfigurationSetting(ConfigLiterals.tgClientRetryPeriodTag, "500", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.tgServerRetryPeriodTag, "500", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.tgRetryCountTag, "10", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.kafkaSubscriberTimeoutTag, "100", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.lowWatermark, "100", ConfigLiterals.systemDomain))

  configService.save(new ConfigurationSetting(ConfigLiterals.geoIpAsNum, "GeoIPASNum.dat", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.geoIpAsNumv6, "GeoIPASNumv6.dat", ConfigLiterals.systemDomain))
}

object TempHelperForConfigDestroy extends App {
  ConnectionRepository.getConfigService.delete(ConfigLiterals.transactionGeneratorTag)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.frameworkTag)
  ConnectionRepository.getConfigService.delete(createConfigurationSettingName(ConfigLiterals.systemDomain, "regular-streaming-validator-class"))
  ConnectionRepository.getConfigService.delete(createConfigurationSettingName(ConfigLiterals.systemDomain, "batch-streaming-validator-class"))
  ConnectionRepository.getConfigService.delete(createConfigurationSettingName(ConfigLiterals.systemDomain, "output-streaming-validator-class"))
  ConnectionRepository.getConfigService.delete(createConfigurationSettingName(ConfigLiterals.systemDomain, "input-streaming-validator-class"))
  ConnectionRepository.getConfigService.delete(ConfigLiterals.marathonTag)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.marathonTimeoutTag)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.zkSessionTimeoutTag)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.tgClientRetryPeriodTag)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.tgServerRetryPeriodTag)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.tgRetryCountTag)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.kafkaSubscriberTimeoutTag)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.lowWatermark)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.geoIpAsNum)
  ConnectionRepository.getConfigService.delete(ConfigLiterals.geoIpAsNumv6)
}