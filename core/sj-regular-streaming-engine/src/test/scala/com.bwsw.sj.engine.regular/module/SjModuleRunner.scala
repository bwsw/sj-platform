package com.bwsw.sj.engine.regular.module

import java.io.File
import java.util.logging.LogManager

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.regular.module.DataFactory._
import com.bwsw.sj.engine.regular.RegularTaskRunner

object SjModuleSetup extends App {
  LogManager.getLogManager.reset()
  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage = ConnectionRepository.getFileStorage
  val checkpointInterval = 4
  val stateManagement = "ram"
  val stateFullCheckpoint = 3
  val _type = "both"

  val module = new File("./contrib/stubs/sj-stub-regular-streaming/target/scala-2.11/sj-stub-regular-streaming_2.11-1.0.0.jar")

  open()
  cassandraSetup()
  loadModule(module, fileStorage)
  createProviders(providerService)
  createServices(serviceManager, providerService)
  createStreams(streamService, serviceManager, partitions, _type, inputCount, outputCount)
  createInstance(serviceManager, instanceService, checkpointInterval, stateManagement, stateFullCheckpoint)

  createData(12, 4, streamService, _type, inputCount)
  close()
  ConnectionRepository.close()

  println("DONE")
}

object SjModuleRunner extends App {
  LogManager.getLogManager.reset()
  RegularTaskRunner.main(Array())
}

object SjModuleDestroy extends App {
  LogManager.getLogManager.reset()
  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage = ConnectionRepository.getFileStorage
  val _type = "both"

  val module = new File("./contrib/stubs/sj-stub-regular-streaming/target/scala-2.11/sj-stub-regular-streaming-1.0.jar")

  open()
  deleteStreams(streamService, _type, inputCount, outputCount)
  deleteServices(serviceManager)
  deleteProviders(providerService)
  deleteInstance(instanceService)
  deleteModule(fileStorage, module.getName)
  cassandraDestroy()
  close()
  ConnectionRepository.close()

  println("DONE")
}

