package com.bwsw

import com.bwsw.sj.common.DAL.model.ConfigElement
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.ConfigConstants

object TempHelperForConfigSetup extends App{

  val configFileService = ConnectionRepository.getConfigFileService

  configFileService.save(new ConfigElement("com.bwsw.tg-0.1", "sj-transaction-generator-assembly-1.0.jar"))
  configFileService.save(new ConfigElement(ConfigConstants.transactionGeneratorTag, "com.bwsw.tg-0.1"))

  configFileService.save(new ConfigElement("com.bwsw.mf-0.1", "ScalaMesos-assembly-1.0.jar"))
  configFileService.save(new ConfigElement(ConfigConstants.frameworkTag, "com.bwsw.mf-0.1"))

  configFileService.save(new ConfigElement("regular-streaming-validator-class", "com.bwsw.sj.crud.rest.validator.module.RegularStreamingValidator"))
  configFileService.save(new ConfigElement("windowed-streaming-validator-class", "com.bwsw.sj.crud.rest.validator.module.WindowedStreamingValidator"))
  configFileService.save(new ConfigElement("output-streaming-validator-class", "com.bwsw.sj.crud.rest.validator.module.OutputStreamingValidator"))

  configFileService.save(new ConfigElement(ConfigConstants.marathonTag, "http://stream-juggler.z1.netpoint-dc.com:8080"))
}
