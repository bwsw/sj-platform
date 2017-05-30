package com.bwsw.sj.engine.regular.module

import java.io.File
import java.util.logging.LogManager

import com.bwsw.sj.common.config.TempHelperForConfigSetup
import com.bwsw.sj.engine.regular.module.DataFactory._

object SjRegularModuleSetup extends App {
  LogManager.getLogManager.reset()
  TempHelperForConfigSetup.main(Array())
  val streamService = connectionRepository.getStreamRepository
  val serviceManager = connectionRepository.getServiceRepository
  val providerService = connectionRepository.getProviderRepository
  val instanceService = connectionRepository.getInstanceRepository
  val fileStorage = connectionRepository.getFileStorage
  val checkpointInterval = 2
  val stateManagement = "ram"
  val stateFullCheckpoint = 2
  val _type = commonMode

  val module = new File("./contrib/stubs/sj-stub-regular-streaming/target/scala-2.12/sj-stub-regular-streaming-1.0-SNAPSHOT.jar")

  loadModule(module, fileStorage)
  createProviders(providerService)
  createServices(serviceManager, providerService)
  createStreams(streamService, serviceManager, partitions, _type, inputCount, outputCount)
  createInstance(serviceManager, instanceService, checkpointInterval, stateManagement, stateFullCheckpoint)

  createData(4, 1, partitions, _type, inputCount)
  connectionRepository.close()

  println("DONE")
}
