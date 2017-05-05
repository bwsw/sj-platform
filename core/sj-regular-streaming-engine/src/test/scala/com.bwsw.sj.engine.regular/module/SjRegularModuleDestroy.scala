package com.bwsw.sj.engine.regular.module

import java.io.File
import java.util.logging.LogManager

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.config.TempHelperForConfigDestroy
import DataFactory._

object SjRegularModuleDestroy extends App {
  LogManager.getLogManager.reset()
  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage = ConnectionRepository.getFileStorage
  val _type = commonMode

  val module = new File("./contrib/stubs/sj-stub-regular-streaming/target/scala-2.12/sj-stub-regular-streaming-1.0-SNAPSHOT.jar")

  deleteStreams(streamService, _type, serviceManager, inputCount, outputCount)
  deleteServices(serviceManager)
  deleteProviders(providerService)
  deleteInstance(instanceService)
  deleteModule(fileStorage, module.getName)

  TempHelperForConfigDestroy.main(Array())
  ConnectionRepository.close()

  println("DONE")
}
