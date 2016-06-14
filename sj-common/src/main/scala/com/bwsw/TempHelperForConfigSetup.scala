package com.bwsw

import com.bwsw.sj.common.ConfigConstants
import com.bwsw.sj.common.DAL.model.ConfigSetting
import com.bwsw.sj.common.DAL.repository.ConnectionRepository

object TempHelperForConfigSetup extends App{

  val configService = ConnectionRepository.getConfigService

  configService.save(new ConfigSetting("com.bwsw.tg-0.1", "sj-transaction-generator-assembly-1.0.jar", "system"))
  configService.save(new ConfigSetting(ConfigConstants.transactionGeneratorTag, "com.bwsw.tg-0.1", "system"))

  configService.save(new ConfigSetting("com.bwsw.mf-0.1", "ScalaMesos-assembly-1.0.jar", "system"))
  configService.save(new ConfigSetting(ConfigConstants.frameworkTag, "com.bwsw.mf-0.1", "system"))

  configService.save(new ConfigSetting("com.bwsw.regular.streaming.engine-0.1", "sj-regular-streaming-engine-assembly-1.0.jar", "system"))
  configService.save(new ConfigSetting(ConfigConstants.regularEngineTag, "com.bwsw.regular.streaming.engine-0.1", "system"))

  configService.save(new ConfigSetting("regular-streaming-validator-class", "com.bwsw.sj.crud.rest.validator.module.RegularStreamingValidator", "system"))
  configService.save(new ConfigSetting("windowed-streaming-validator-class", "com.bwsw.sj.crud.rest.validator.module.WindowedStreamingValidator", "system"))
  configService.save(new ConfigSetting("output-streaming-validator-class", "com.bwsw.sj.crud.rest.validator.module.OutputStreamingValidator", "system"))

  configService.save(new ConfigSetting(ConfigConstants.marathonTag, "http://stream-juggler.z1.netpoint-dc.com:8080", "system"))
}
