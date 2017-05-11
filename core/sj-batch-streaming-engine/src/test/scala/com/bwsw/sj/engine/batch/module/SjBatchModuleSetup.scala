package com.bwsw.sj.engine.batch.module

import java.io.File
import java.util.logging.LogManager

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.config.TempHelperForConfigSetup
import com.bwsw.sj.engine.batch.module.DataFactory._

object SjBatchModuleSetup extends App {
  LogManager.getLogManager.reset()
  TempHelperForConfigSetup.main(Array())
  val streamService = ConnectionRepository.getStreamRepository
  val serviceManager = ConnectionRepository.getServiceRepository
  val providerService = ConnectionRepository.getProviderRepository
  val instanceService = ConnectionRepository.getInstanceRepository
  val fileStorage = ConnectionRepository.getFileStorage
  val stateManagement = "ram"
  val stateFullCheckpoint = 3
  val window = 2
  val slidingInterval = 1
  val _type = commonMode

  val module = new File("./contrib/stubs/sj-stub-batch-streaming/target/scala-2.12/sj-stub-batch-streaming-1.0-SNAPSHOT.jar")

  loadModule(module, fileStorage)
  createProviders(providerService)
  createServices(serviceManager, providerService)
  createStreams(streamService, serviceManager, partitions, _type, inputCount, outputCount)
  createInstance(serviceManager, instanceService, window, slidingInterval, stateManagement, stateFullCheckpoint)

  createData(6, 1, streamService, _type, inputCount)
  ConnectionRepository.close()

  println("DONE")
}
