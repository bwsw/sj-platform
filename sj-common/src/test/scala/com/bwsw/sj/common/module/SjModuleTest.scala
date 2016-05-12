package com.bwsw.sj.common.module

import java.io.File

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.module.DataFactory._

object SjModuleTest extends App {

  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage = ConnectionRepository.getFileStorage

  val module = new File("/home/mikhaleva_ka/Juggler/sj-stub-module/target/scala-2.11/sj-stub-module-test.jar")

  cassandraSetup()
  loadModule(module, fileStorage)
  createProviders(providerService)
  createServices(serviceManager, providerService)
  createStreams(streamService, serviceManager)
  createTStreams()
  createInstance(instanceService)

  deleteStreams(streamService)
  deleteServices(serviceManager)
  deleteProviders(providerService)
  deleteInstance(instanceService)
  deleteTStreams()
  deleteModule(fileStorage, module.getName)
  close()

  ConnectionRepository.close()
  println("DONE")
}
