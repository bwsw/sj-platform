package com.bwsw.sj.engine.input

import java.io.File
import java.util.logging.LogManager

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.TempHelperForConfigSetup
import com.bwsw.sj.engine.input.DataFactory._


object SjInputInfoExp {
  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage = ConnectionRepository.getFileStorage

  val inputModule = new File("./contrib/stubs/sj-stub-input-streaming/target/scala-2.12/sj-stub-input-streaming-1.0-SNAPSHOT.jar")
}

object SjInputModuleSetup extends App {
  LogManager.getLogManager.reset()
  TempHelperForConfigSetup.main(Array())

  val checkpointInterval = 10

  loadModule(SjInputInfoExp.inputModule, SjInputInfoExp.fileStorage)
  createProviders(SjInputInfoExp.providerService)
  createServices(SjInputInfoExp.serviceManager, SjInputInfoExp.providerService)
  createStreams(SjInputInfoExp.streamService, SjInputInfoExp.serviceManager, outputCount)
  createInstance(SjInputInfoExp.serviceManager, SjInputInfoExp.instanceService, checkpointInterval)
  ConnectionRepository.close()

  println("DONE")
}

object SjInputModuleRunner extends App {
  LogManager.getLogManager.reset()
  InputTaskRunner.main(Array())
}

object SjInputDataWriterRunner extends App {
  LogManager.getLogManager.reset()
  writeData(15, 5)

  ConnectionRepository.close()
}

object DuplicateCheckerRunner extends App {
  LogManager.getLogManager.reset()
  DuplicateChecker.main(Array("15", "5"))
}

object SjInputModuleDestroy extends App {
  LogManager.getLogManager.reset()

  deleteStreams(SjInputInfoExp.streamService, outputCount)
  deleteServices(SjInputInfoExp.serviceManager)
  deleteProviders(SjInputInfoExp.providerService)
  deleteInstance(SjInputInfoExp.instanceService)
  deleteModule(SjInputInfoExp.fileStorage, SjInputInfoExp.inputModule.getName)
  ConnectionRepository.getConfigService.deleteAll()
  ConnectionRepository.close()

  println("DONE")
}
