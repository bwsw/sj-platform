package com.bwsw.sj.engine.regular.module

import java.io.File
import java.util.logging.LogManager

import com.bwsw.sj.common.config.TempHelperForConfigDestroy
import DataFactory._

object SjRegularModuleDestroy extends App {
  LogManager.getLogManager.reset()
  val streamService = connectionRepository.getStreamRepository
  val serviceManager = connectionRepository.getServiceRepository
  val providerService = connectionRepository.getProviderRepository
  val instanceService = connectionRepository.getInstanceRepository
  val fileStorage = connectionRepository.getFileStorage
  val _type = commonMode

  val module = new File("./contrib/stubs/sj-stub-regular-streaming/target/scala-2.12/sj-stub-regular-streaming-1.0-SNAPSHOT.jar")

  deleteStreams(streamService, _type, serviceManager, inputCount, outputCount)
  deleteServices(serviceManager)
  deleteProviders(providerService)
  deleteInstance(instanceService)
  deleteModule(fileStorage, module.getName)

  TempHelperForConfigDestroy.main(Array())
  connectionRepository.close()

  println("DONE")
}
