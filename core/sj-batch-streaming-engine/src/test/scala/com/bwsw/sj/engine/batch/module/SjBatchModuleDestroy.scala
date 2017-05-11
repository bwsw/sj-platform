package com.bwsw.sj.engine.batch.module

import java.io.File
import java.util.logging.LogManager

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.config.TempHelperForConfigDestroy
import com.bwsw.sj.engine.batch.module.DataFactory._

object SjBatchModuleDestroy extends App {
  LogManager.getLogManager.reset()
  val streamService = ConnectionRepository.getStreamRepository
  val serviceManager = ConnectionRepository.getServiceRepository
  val providerService = ConnectionRepository.getProviderRepository
  val instanceService = ConnectionRepository.getInstanceRepository
  val fileStorage = ConnectionRepository.getFileStorage
  val _type = commonMode

  val module = new File("./contrib/stubs/sj-stub-batch-streaming/target/scala-2.12/sj-stub-batch-streaming-1.0-SNAPSHOT.jar")

  deleteStreams(streamService, _type, serviceManager, inputCount, outputCount)
  deleteServices(serviceManager)
  deleteProviders(providerService)
  deleteInstance(instanceService)
  deleteModule(fileStorage, module.getName)

  TempHelperForConfigDestroy.main(Array())
  ConnectionRepository.close()

  println("DONE")
}
