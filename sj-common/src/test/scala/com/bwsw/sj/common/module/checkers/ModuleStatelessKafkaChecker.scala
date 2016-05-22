package com.bwsw.sj.common.module.checkers

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.module.DataFactory._
import scala.collection.JavaConverters._

object ModuleStatelessKafkaChecker extends App {

  val streamService = ConnectionRepository.getStreamService
  val objectSerializer = new ObjectSerializer()

  val inputConsumer = createInputKafkaConsumer(streamService)
  val outputConsumers = (1 to outputCount).map(x => createOutputConsumer(streamService, x.toString))

  var totalInputElements = 0
  var totalOutputElements = 0

  var inputElements = scala.collection.mutable.ArrayBuffer[Int]()
  var outputElements = scala.collection.mutable.ArrayBuffer[Int]()

  var records = inputConsumer.poll(100 * 60)

  records.asScala.foreach(x => {
    val bytes = x.value()
    val element = objectSerializer.deserialize(bytes).asInstanceOf[Int]
    inputElements.+=(element)
    totalInputElements += 1
  })

  outputConsumers.foreach(outputConsumer => {
    var maybeTxn = outputConsumer.getTransaction

    while (maybeTxn.isDefined) {
      val txn = maybeTxn.get
      while (txn.hasNext()) {
        val element = objectSerializer.deserialize(txn.next()).asInstanceOf[Int]
        outputElements.+=(element)
        totalOutputElements += 1
      }
      maybeTxn = outputConsumer.getTransaction
    }
  })

  assert(totalInputElements == totalOutputElements,
    "Count of all txns elements that are consumed from output stream should equals count of all txns elements that are consumed from input stream")

  assert(inputElements.forall(x => outputElements.contains(x)) && outputElements.forall(x => inputElements.contains(x)),
    "All txns elements that are consumed from output stream should equals all txns elements that are consumed from input stream")

  close()
  ConnectionRepository.close()
  outputConsumers.foreach(_.stop())

  println("DONE")
}
