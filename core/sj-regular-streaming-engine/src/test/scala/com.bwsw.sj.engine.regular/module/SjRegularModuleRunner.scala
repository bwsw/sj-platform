package com.bwsw.sj.engine.regular.module

import java.io.File
import java.util.logging.LogManager

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.{TempHelperForConfigDestroy, TempHelperForConfigSetup}
import com.bwsw.sj.engine.regular.module.DataFactory._
import com.bwsw.sj.engine.regular.RegularTaskRunner

object SjRegularModuleSetup extends App {
  LogManager.getLogManager.reset()
  TempHelperForConfigSetup.main(Array())
  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage = ConnectionRepository.getFileStorage
  val checkpointInterval = 2
  val stateManagement = "ram"
  val stateFullCheckpoint = 2
  val _type = commonMode

  val module = new File("./contrib/stubs/sj-stub-regular-streaming/target/scala-2.12/sj-stub-regular-streaming-1.0-SNAPSHOT.jar")

  open()
  cassandraSetup()
  loadModule(module, fileStorage)
  createProviders(providerService)
  createServices(serviceManager, providerService)
  createStreams(streamService, serviceManager, partitions, _type, inputCount, outputCount)
  createInstance(serviceManager, instanceService, checkpointInterval, stateManagement, stateFullCheckpoint)

  createData(4, 4, partitions, _type, inputCount)
  close()
  ConnectionRepository.close()

  println("DONE")
}

object SjRegularModuleRunner extends App {
  LogManager.getLogManager.reset()
  RegularTaskRunner.main(Array())
}

object SjRegularModuleDestroy extends App {
  LogManager.getLogManager.reset()
  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage = ConnectionRepository.getFileStorage
  val _type = commonMode

  val module = new File("./contrib/stubs/sj-stub-regular-streaming/target/scala-2.12/sj-stub-regular-streaming-1.0-SNAPSHOT.jar")

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

