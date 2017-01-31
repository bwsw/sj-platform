package com.bwsw.sj.engine.windowed.module

import java.io.File
import java.util.logging.LogManager

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.{TempHelperForConfigDestroy, TempHelperForConfigSetup}
import com.bwsw.sj.engine.windowed.WindowedTaskRunner
import com.bwsw.sj.engine.windowed.module.DataFactory._

object SjWindowedModuleSetup extends App {
  LogManager.getLogManager.reset()
  TempHelperForConfigSetup.main(Array())
  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage = ConnectionRepository.getFileStorage
  val stateManagement = "ram"
  val stateFullCheckpoint = 3
  val batchCheckpointInterval = 2
  val window = 4
  val slidingInterval = 2
  val _type = "both"

  val module = new File("./contrib/stubs/sj-stub-windowed-streaming/target/scala-2.12/sj-stub-windowed-streaming-1.0-SNAPSHOT.jar")

  open()
  cassandraSetup()
  loadModule(module, fileStorage)
  createProviders(providerService)
  createServices(serviceManager, providerService)
  createStreams(streamService, serviceManager, partitions, _type, inputCount, outputCount)
  createInstance(serviceManager, instanceService, batchCheckpointInterval, window, slidingInterval, stateManagement, stateFullCheckpoint)

  createData(12, 1, streamService, _type, inputCount)
  close()
  ConnectionRepository.close()

  println("DONE")
}

object SjWindowedModuleRunner extends App {
  LogManager.getLogManager.reset()
  WindowedTaskRunner.main(Array())
}

object SjWindowedModuleDestroy extends App {
  LogManager.getLogManager.reset()
  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage = ConnectionRepository.getFileStorage
  val _type = "both"

  val module = new File("./contrib/stubs/sj-stub-windowed-streaming/target/scala-2.12/sj-stub-windowed-streaming-1.0-SNAPSHOT.jar")

  open()
  deleteStreams(streamService, _type, inputCount, outputCount)
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
