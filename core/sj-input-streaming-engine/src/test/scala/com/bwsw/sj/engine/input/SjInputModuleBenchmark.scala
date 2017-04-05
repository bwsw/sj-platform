package com.bwsw.sj.engine.input

import java.io.File
import java.util.logging.LogManager

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.TempHelperForConfigSetup
import com.bwsw.sj.engine.input.DataFactory._
import SjInputServices._

object SjInputServices {
  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage = ConnectionRepository.getFileStorage

  val inputModule = new File("./contrib/stubs/sj-stub-input-streaming/target/scala-2.12/sj-stub-input-streaming-1.0-SNAPSHOT.jar")
  val checkpointInterval = 10
  val numberOfDuplicates = 10
  val totalInputElements = 2 * checkpointInterval + numberOfDuplicates // increase/decrease a constant to change the number of input elements
}

object SjInputModuleSetup extends App {
  LogManager.getLogManager.reset()
  TempHelperForConfigSetup.main(Array())

  open()
  cassandraSetup()
  loadModule(SjInputServices.inputModule, SjInputServices.fileStorage)
  createProviders(SjInputServices.providerService)
  createServices(SjInputServices.serviceManager, SjInputServices.providerService)
  createStreams(SjInputServices.streamService, SjInputServices.serviceManager, outputCount)
  createInstance(SjInputServices.serviceManager, SjInputServices.instanceService, checkpointInterval)
  close()
  ConnectionRepository.close()

  println("DONE")
}

object SjInputModuleRunner extends App {
  LogManager.getLogManager.reset()
  InputTaskRunner.main(Array())
}

object SjInputModuleDataWriter extends App {
  LogManager.getLogManager.reset()
  writeData(totalInputElements, numberOfDuplicates)

  ConnectionRepository.close()
}

object SjInputModuleDuplicateCheckerRunner extends App {
  LogManager.getLogManager.reset()
  DuplicateChecker.main(Array(totalInputElements.toString, numberOfDuplicates.toString))
}

object SjInputModuleDestroy extends App {
  LogManager.getLogManager.reset()

  open()
  deleteStreams(SjInputServices.streamService, outputCount)
  deleteServices(SjInputServices.serviceManager)
  deleteProviders(SjInputServices.providerService)
  deleteInstance(SjInputServices.instanceService)
  deleteModule(SjInputServices.fileStorage, SjInputServices.inputModule.getName)
  cassandraDestroy()
  close()
  ConnectionRepository.getConfigService.deleteAll()
  ConnectionRepository.close()

  println("DONE")
}
