package com.bwsw.sj.common.module

import java.io.File

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.module.DataFactory._
import com.bwsw.sj.common.module.regular.RegularTaskRunner

object SjModuleSetup extends App {

  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage = ConnectionRepository.getFileStorage
  val partitions = 3
  val checkpointInterval = 4
  val stateManagement = "ram"
  val stateFullCheckpoint = 3
  val _type = "both"

  val module = new File("/home/mikhaleva_ka/Juggler/sj-stub-module/target/scala-2.11/sj-stub-module-test.jar")

  cassandraSetup()
  loadModule(module, fileStorage)
  createProviders(providerService)
  createServices(serviceManager, providerService)
  createStreams(streamService, serviceManager, partitions, _type)
  createInstance(instanceService, checkpointInterval, stateManagement, stateFullCheckpoint)

  createData(32, 4, streamService, _type)

  close()
  ConnectionRepository.close()

  println("DONE")
}

object SjModuleRunner extends App {
  RegularTaskRunner.main(Array())
}

object SjModuleDestroy extends App {
  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage = ConnectionRepository.getFileStorage
  val _type = "both"

  val module = new File("/home/mikhaleva_ka/Juggler/sj-stub-module/target/scala-2.11/sj-stub-module-test.jar")

  deleteStreams(streamService, _type)
  deleteServices(serviceManager)
  deleteProviders(providerService)
  deleteInstance(instanceService)
  deleteModule(fileStorage, module.getName)
  cassandraDestroy()

  close()
  ConnectionRepository.close()

  println("DONE")
}

