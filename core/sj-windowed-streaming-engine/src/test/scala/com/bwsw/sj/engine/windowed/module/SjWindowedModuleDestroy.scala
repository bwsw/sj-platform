package com.bwsw.sj.engine.windowed.module

import java.io.File
import java.util.logging.LogManager

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.TempHelperForConfigDestroy
import com.bwsw.sj.engine.windowed.module.DataFactory._

object SjWindowedModuleDestroy extends App {
  LogManager.getLogManager.reset()
  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage = ConnectionRepository.getFileStorage
  val _type = commonMode

  val module = new File("./contrib/stubs/sj-stub-windowed-streaming/target/scala-2.12/sj-stub-windowed-streaming-1.0-SNAPSHOT.jar")

  open()
  deleteStreams(streamService, _type, serviceManager, inputCount, outputCount)
  deleteServices(serviceManager)
  deleteProviders(providerService)
  deleteInstance(instanceService)
  deleteModule(fileStorage, module.getName)
  cassandraDestroy()
  close()
  TempHelperForConfigDestroy.main(Array())
  ConnectionRepository.close()

  println("DONE")
}
