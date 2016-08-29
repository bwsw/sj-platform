package com.bwsw.sj.common.utils

import java.io.File

import com.bwsw.sj.common.DAL.model.ConfigSetting
import com.bwsw.sj.common.DAL.repository.ConnectionRepository

object TempHelperForConfigSetup extends App{

  ConnectionRepository.getFileStorage.put(new File("GeoIPASNum.dat"), "GeoIPASNum.dat")
  ConnectionRepository.getFileStorage.put(new File("GeoIPASNumv6.dat"), "GeoIPASNumv6.dat")

  val configService = ConnectionRepository.getConfigService

  configService.save(new ConfigSetting(ConfigConstants.transactionGeneratorTag, "com.bwsw.tg-1.0", "system"))

  configService.save(new ConfigSetting(ConfigConstants.frameworkTag, "com.bwsw.fw-1.0", "system"))

  configService.save(new ConfigSetting("system" + "." + "regular-streaming-validator-class", "com.bwsw.sj.crud.rest.validator.module.RegularStreamingValidator", "system"))
  configService.save(new ConfigSetting("system" + "." + "windowed-streaming-validator-class", "com.bwsw.sj.crud.rest.validator.module.WindowedStreamingValidator", "system"))
  configService.save(new ConfigSetting("system" + "." + "output-streaming-validator-class", "com.bwsw.sj.crud.rest.validator.module.OutputStreamingValidator", "system"))
  configService.save(new ConfigSetting("system" + "." + "input-streaming-validator-class", "com.bwsw.sj.crud.rest.validator.module.InputStreamingValidator", "system"))

  configService.save(new ConfigSetting(ConfigConstants.marathonTag, "http://stream-juggler.z1.netpoint-dc.com:8080", "system"))
  configService.save(new ConfigSetting(ConfigConstants.marathonTimeoutTag, "60000", "system"))

  configService.save(new ConfigSetting(ConfigConstants.zkSessionTimeoutTag, "7000", "zk"))

  //configService.save(new ConfigSetting("session.timeout.ms", "30000", "kafka")) // for kafka domain
  configService.save(new ConfigSetting(ConfigConstants.jdbcTimeoutTag, "6000", "jdbc"))

  configService.save(new ConfigSetting(ConfigConstants.tgClientRetryPeriodTag, "500", "system"))
  configService.save(new ConfigSetting(ConfigConstants.tgServerRetryPeriodTag, "500", "system"))
  configService.save(new ConfigSetting(ConfigConstants.tgRetryCountTag, "10", "system"))
  configService.save(new ConfigSetting(ConfigConstants.kafkaSubscriberTimeoutTag, "10", "system"))

  configService.save(new ConfigSetting(ConfigConstants.geoIpAsNum, "GeoIPASNum.dat", "system"))
  configService.save(new ConfigSetting(ConfigConstants.geoIpAsNumv6, "GeoIPASNumv6.dat", "system"))
}