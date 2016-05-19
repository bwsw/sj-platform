package com.bwsw.sj.common.module.checkers

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.module.DataFactory._
import scala.collection.JavaConverters._

object ModuleStatelessChecker extends App {

  val streamService = ConnectionRepository.getStreamService
  val objectSerializer = new ObjectSerializer()

  val inputTstreamConsumer = createInputTstreamConsumer(streamService)
  val inputKafkaConsumer = createInputKafkaConsumer(streamService)
  val outputConsumer = createOutputConsumer(streamService)

  var totalInputElements = 0
  var totalOutputElements = 0

  var inputElements = scala.collection.mutable.ArrayBuffer[Int]()
  var outputElements = scala.collection.mutable.ArrayBuffer[Int]()

  var maybeTxn = inputTstreamConsumer.getTransaction
  while (maybeTxn.isDefined) {
    val txn = maybeTxn.get
    while (txn.hasNext()) {
      val element = objectSerializer.deserialize(txn.next()).asInstanceOf[Int]
      inputElements.+=(element)
      totalInputElements += 1
    }
    maybeTxn = inputTstreamConsumer.getTransaction
  }

  var records = inputKafkaConsumer.poll(100 * 60)
  records.asScala.foreach(x => {
    val bytes = x.value()
    val element = objectSerializer.deserialize(bytes).asInstanceOf[Int]
    inputElements.+=(element)
    totalInputElements += 1
  })

  maybeTxn = outputConsumer.getTransaction
  while (maybeTxn.isDefined) {
    val txn = maybeTxn.get
    while (txn.hasNext()) {
      val element = objectSerializer.deserialize(txn.next()).asInstanceOf[Int]
      outputElements.+=(element)
      totalOutputElements += 1
    }
    maybeTxn = outputConsumer.getTransaction
  }

  assert(totalInputElements == totalOutputElements,
    "Count of all txns elements that are consumed from output stream should equals count of all txns elements that are consumed from input stream")

  assert(inputElements.forall(x => outputElements.contains(x)) && outputElements.forall(x => inputElements.contains(x)),
    "All txns elements that are consumed from output stream should equals all txns elements that are consumed from input stream")

  close()
  ConnectionRepository.close()

  println("DONE")
}
