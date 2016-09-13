package com.bwsw.sj.common.utils

import java.io.File

import com.bwsw.sj.common.DAL.model.ConfigurationSetting
import com.bwsw.sj.common.DAL.repository.ConnectionRepository

object TempHelperForConfigSetup extends App {

  ConnectionRepository.getFileStorage.put(new File("GeoIPASNum.dat"), "GeoIPASNum.dat")
  ConnectionRepository.getFileStorage.put(new File("GeoIPASNumv6.dat"), "GeoIPASNumv6.dat")

  val configService = ConnectionRepository.getConfigService

  configService.save(new ConfigurationSetting(ConfigConstants.transactionGeneratorTag, "com.bwsw.tg-1.0", ConfigConstants.systemDomain))

  configService.save(new ConfigurationSetting(ConfigConstants.frameworkTag, "com.bwsw.fw-1.0", ConfigConstants.systemDomain))

  configService.save(new ConfigurationSetting(ConfigConstants.systemDomain + "." + "regular-streaming-validator-class",
    "com.bwsw.sj.crud.rest.validator.module.RegularStreamingValidator", ConfigConstants.systemDomain))
  configService.save(new ConfigurationSetting(ConfigConstants.systemDomain + "." + "windowed-streaming-validator-class",
    "com.bwsw.sj.crud.rest.validator.module.WindowedStreamingValidator", ConfigConstants.systemDomain))
  configService.save(new ConfigurationSetting(ConfigConstants.systemDomain + "." + "output-streaming-validator-class",
    "com.bwsw.sj.crud.rest.validator.module.OutputStreamingValidator", ConfigConstants.systemDomain))
  configService.save(new ConfigurationSetting(ConfigConstants.systemDomain + "." + "input-streaming-validator-class",
    "com.bwsw.sj.crud.rest.validator.module.InputStreamingValidator", ConfigConstants.systemDomain))

  configService.save(new ConfigurationSetting(ConfigConstants.marathonTag, "http://stream-juggler.z1.netpoint-dc.com:8080", ConfigConstants.systemDomain))
  configService.save(new ConfigurationSetting(ConfigConstants.marathonTimeoutTag, "60000", ConfigConstants.systemDomain))

  configService.save(new ConfigurationSetting(ConfigConstants.zkSessionTimeoutTag, "7000", ConfigConstants.zookeeperDomain))

  //configService.save(new ConfigurationSetting("session.timeout.ms", "30000", ConfigConstants.kafkaDomain))
  configService.save(new ConfigurationSetting(ConfigConstants.jdbcTimeoutTag, "6000", ConfigConstants.jdbcDomain))

  configService.save(new ConfigurationSetting(ConfigConstants.tgClientRetryPeriodTag, "500", ConfigConstants.systemDomain))
  configService.save(new ConfigurationSetting(ConfigConstants.tgServerRetryPeriodTag, "500", ConfigConstants.systemDomain))
  configService.save(new ConfigurationSetting(ConfigConstants.tgRetryCountTag, "10", ConfigConstants.systemDomain))
  configService.save(new ConfigurationSetting(ConfigConstants.kafkaSubscriberTimeoutTag, "10", ConfigConstants.systemDomain))

  configService.save(new ConfigurationSetting(ConfigConstants.geoIpAsNum, "GeoIPASNum.dat", ConfigConstants.systemDomain))
  configService.save(new ConfigurationSetting(ConfigConstants.geoIpAsNumv6, "GeoIPASNumv6.dat", ConfigConstants.systemDomain))
}