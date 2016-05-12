package com.bwsw.sj.common.module

import java.io.File

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.module.DataFactory._
import com.bwsw.sj.common.module.regular.RegularTaskRunner
import org.scalatest.{Matchers, FlatSpec}

object SjModuleSetup extends App {

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

  createData(5, 5, streamService)

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

  val module = new File("/home/mikhaleva_ka/Juggler/sj-stub-module/target/scala-2.11/sj-stub-module-test.jar")

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

object SjModuleTest extends FlatSpec with App with Matchers {
  val streamService = ConnectionRepository.getStreamService
  val objectSerializer = new ObjectSerializer()

  "Count of all txns elements that are consumed from output stream" should "equals count of all txns elements that are consumed from input stream" in {
    val inputConsumer = createInputConsumer(streamService)
    val outputConsumer = createOutputConsumer(streamService)

    var totalInputElements = 0
    var totalOutputElements = 0

    var maybeTxn = inputConsumer.getTransaction
    while (maybeTxn.isDefined) {
      val txn = maybeTxn.get
      while (txn.hasNext()) {
        txn.next()
        totalInputElements += 1
      }
      maybeTxn = inputConsumer.getTransaction
    }

    maybeTxn = outputConsumer.getTransaction
    while (maybeTxn.isDefined) {
      val txn = maybeTxn.get
      while (txn.hasNext()) {
        txn.next()
        totalOutputElements += 1
      }
      maybeTxn = outputConsumer.getTransaction
    }

    totalInputElements shouldEqual totalOutputElements
  }

  "All txns elements that are consumed from output stream" should "equals all txns elements that are consumed from input stream" in {
    val inputConsumer = createInputConsumer(streamService)
    val outputConsumer = createOutputConsumer(streamService)

    var inputElements = scala.collection.mutable.ArrayBuffer[Int]()
    var outputElements = scala.collection.mutable.ArrayBuffer[Int]()

    var maybeTxn = inputConsumer.getTransaction
    while (maybeTxn.isDefined) {
      val txn = maybeTxn.get
      while (txn.hasNext()) {
        val element = objectSerializer.deserialize(txn.next()).asInstanceOf[String].toInt
        inputElements.+=(element)
      }
      maybeTxn = inputConsumer.getTransaction
    }

    maybeTxn = outputConsumer.getTransaction
    while (maybeTxn.isDefined) {
      val txn = maybeTxn.get
      while (txn.hasNext()) {
        val element = objectSerializer.deserialize(txn.next()).asInstanceOf[String].toInt
        outputElements.+=(element)
      }
      maybeTxn = outputConsumer.getTransaction
    }

    inputElements should contain theSameElementsAs outputElements
  }
}