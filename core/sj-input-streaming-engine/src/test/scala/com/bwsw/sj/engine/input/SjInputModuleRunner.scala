package com.bwsw.sj.engine.input

import java.io.File
import java.util.logging.LogManager

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.input.DataFactory._

object SjInputModuleSetup extends App {
  LogManager.getLogManager.reset()
  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage = ConnectionRepository.getFileStorage
  val checkpointInterval = 10

  val inputModule = new File("./contrib/stubs/sj-stub-input-streaming/target/scala-2.11/sj-stub-input-streaming-1.0.jar")

  cassandraSetup()
  loadModule(inputModule, fileStorage)
  createProviders(providerService)
  createServices(serviceManager, providerService)
  createStreams(streamService, serviceManager, outputCount)
  createInstance(serviceManager, instanceService, checkpointInterval)

  close()
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

  close()
  ConnectionRepository.close()
}

object DuplicateCheckerRunner extends App {
  LogManager.getLogManager.reset()
  DuplicateChecker.main(Array("15", "5"))
}

object SjInputModuleDestroy extends App {
  LogManager.getLogManager.reset()
  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage = ConnectionRepository.getFileStorage

  val inputModule = new File("./contrib/stubs/sj-stub-input-streaming/target/scala-2.11/sj-stub-input-streaming-1.0.jar")

  deleteStreams(streamService, outputCount)
  deleteServices(serviceManager)
  deleteProviders(providerService)
  deleteInstance(instanceService)
  deleteModule(fileStorage, inputModule.getName)
  cassandraDestroy()

  close()
  ConnectionRepository.close()

  println("DONE")
}
