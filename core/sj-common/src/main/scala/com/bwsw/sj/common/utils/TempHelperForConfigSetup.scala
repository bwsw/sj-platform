package com.bwsw.sj.common.utils

import com.bwsw.sj.common.DAL.model.ConfigurationSetting
import com.bwsw.sj.common.DAL.repository.ConnectionRepository

object TempHelperForConfigSetup extends App {

  //ConnectionRepository.getFileStorage.put(new File("GeoIPASNum.dat"), "GeoIPASNum.dat")
  //ConnectionRepository.getFileStorage.put(new File("GeoIPASNumv6.dat"), "GeoIPASNumv6.dat")

  val configService = ConnectionRepository.getConfigService

  configService.save(new ConfigurationSetting(ConfigLiterals.transactionGeneratorTag, "com.bwsw.tg-1.0", ConfigLiterals.systemDomain))

  configService.save(new ConfigurationSetting(ConfigLiterals.frameworkTag, "com.bwsw.fw-1.0", ConfigLiterals.systemDomain))

  configService.save(new ConfigurationSetting(ConfigLiterals.systemDomain + "." + "regular-streaming-validator-class",
    "com.bwsw.sj.crud.rest.validator.module.RegularStreamingValidator", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.systemDomain + "." + "windowed-streaming-validator-class",
    "com.bwsw.sj.crud.rest.validator.module.WindowedStreamingValidator", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.systemDomain + "." + "output-streaming-validator-class",
    "com.bwsw.sj.crud.rest.validator.module.OutputStreamingValidator", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.systemDomain + "." + "input-streaming-validator-class",
    "com.bwsw.sj.crud.rest.validator.module.InputStreamingValidator", ConfigLiterals.systemDomain))

  configService.save(new ConfigurationSetting(ConfigLiterals.marathonTag, "http://stream-juggler.z1.netpoint-dc.com:8080", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.marathonTimeoutTag, "60000", ConfigLiterals.systemDomain))

  configService.save(new ConfigurationSetting(ConfigLiterals.zkSessionTimeoutTag, "7000", ConfigLiterals.zookeeperDomain))

  //configService.save(new ConfigurationSetting("session.timeout.ms", "30000", ConfigConstants.kafkaDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.jdbcTimeoutTag, "6000", ConfigLiterals.jdbcDomain))

  configService.save(new ConfigurationSetting(ConfigLiterals.tgClientRetryPeriodTag, "500", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.tgServerRetryPeriodTag, "500", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.tgRetryCountTag, "10", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.kafkaSubscriberTimeoutTag, "100", ConfigLiterals.systemDomain))

  configService.save(new ConfigurationSetting(ConfigLiterals.geoIpAsNum, "GeoIPASNum.dat", ConfigLiterals.systemDomain))
  configService.save(new ConfigurationSetting(ConfigLiterals.geoIpAsNumv6, "GeoIPASNumv6.dat", ConfigLiterals.systemDomain))
}