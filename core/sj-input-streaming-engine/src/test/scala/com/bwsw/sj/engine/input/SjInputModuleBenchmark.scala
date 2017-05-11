package com.bwsw.sj.engine.input

import java.io.{File, PrintStream}
import java.net.Socket
import java.util.logging.LogManager

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.config.TempHelperForConfigSetup
import com.bwsw.sj.engine.input.DataFactory._
import com.bwsw.sj.engine.input.SjInputServices._

import scala.util.{Failure, Success, Try}

object SjInputServices {
  val streamService = ConnectionRepository.getStreamRepository
  val serviceManager = ConnectionRepository.getServiceRepository
  val providerService = ConnectionRepository.getProviderRepository
  val instanceService = ConnectionRepository.getInstanceRepository
  val fileStorage = ConnectionRepository.getFileStorage

  val host = "localhost"
  val port = 8888
  val inputModule = new File("./contrib/stubs/sj-stub-input-streaming/target/scala-2.12/sj-stub-input-streaming-1.0-SNAPSHOT.jar")
  val checkpointInterval = 10
  val numberOfDuplicates = 10
  val totalInputElements = 2 * checkpointInterval + numberOfDuplicates // increase/decrease a constant to change the number of input elements
}

object SjInputModuleSetup extends App {
  LogManager.getLogManager.reset()
  TempHelperForConfigSetup.main(Array())

  loadModule(SjInputServices.inputModule, SjInputServices.fileStorage)
  createProviders(SjInputServices.providerService)
  createServices(SjInputServices.serviceManager, SjInputServices.providerService)
  createStreams(SjInputServices.streamService, SjInputServices.serviceManager, outputCount)
  createInstance(SjInputServices.serviceManager, SjInputServices.instanceService, checkpointInterval)

  ConnectionRepository.close()

  println("DONE")
}

object SjInputModuleRunner extends App {
  InputTaskRunner.main(Array())
}

object SjInputModuleDataWriter extends App {
  LogManager.getLogManager.reset()
  writeData(totalInputElements, numberOfDuplicates)

  private def writeData(totalInputElements: Int, numberOfDuplicates: Int) = {
    Try {
      val socket = new Socket(host, port)
      var amountOfDuplicates = -1
      var amountOfElements = 0
      var currentElement = 1
      val out = new PrintStream(socket.getOutputStream)

      while (amountOfElements < totalInputElements) {
        if (amountOfDuplicates != numberOfDuplicates) {
          out.println(currentElement)
          out.flush()
          amountOfElements += 1
          amountOfDuplicates += 1
        }
        else {
          currentElement += 1
          out.println(currentElement)
          out.flush()
          amountOfElements += 1
        }
      }

      socket.close()
    } match {
      case Success(_) =>
      case Failure(e) =>
        System.out.println("init error: " + e)
    }
  }
}

object SjInputModuleDuplicateCheckerRunner extends App {
  DuplicateChecker.main(Array(totalInputElements.toString, numberOfDuplicates.toString))
}

object SjInputModuleDestroy extends App {
  LogManager.getLogManager.reset()

  deleteStreams(SjInputServices.streamService, outputCount)
  deleteServices(SjInputServices.serviceManager)
  deleteProviders(SjInputServices.providerService)
  deleteInstance(SjInputServices.instanceService)
  deleteModule(SjInputServices.fileStorage, SjInputServices.inputModule.getName)

  ConnectionRepository.getConfigRepository.deleteAll()
  ConnectionRepository.close()

  println("DONE")
}