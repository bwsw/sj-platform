package com.bwsw.sj.common.module.checkers

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.module.DataFactory._
import com.bwsw.sj.common.utils.StateHelper
import com.bwsw.tstreams.agents.consumer.BasicConsumer

object ModuleStatefulTstreamChecker extends App {

  val streamService = ConnectionRepository.getStreamService
  val objectSerializer = new ObjectSerializer()

  val inputTstreamConsumers = (1 to inputCount).map(x => createInputTstreamConsumer(streamService, x.toString))
  val outputConsumers = (1 to outputCount).map(x => createOutputConsumer(streamService, x.toString))

  var totalInputElements = 0
  var totalOutputElements = 0

  var inputElements = scala.collection.mutable.ArrayBuffer[Int]()
  var outputElements = scala.collection.mutable.ArrayBuffer[Int]()

  inputTstreamConsumers.foreach(inputTstreamConsumer => {
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

  val consumer: BasicConsumer[Array[Byte], Array[Byte]] = createStateConsumer(streamService)
  val initialState = StateHelper.getState(consumer, objectSerializer)

  assert(totalInputElements == totalOutputElements,
    "Count of all txns elements that are consumed from output stream should equals count of all txns elements that are consumed from input stream")

  assert(inputElements.forall(x => outputElements.contains(x)) && outputElements.forall(x => inputElements.contains(x)),
    "All txns elements that are consumed from output stream should equals all txns elements that are consumed from input stream")

  assert(initialState("sum") == inputElements.sum,
    "Sum of all txns elements that are consumed from input stream should equals state variable sum")

  close()
  ConnectionRepository.close()
  inputTstreamConsumers.foreach(_.stop())
  outputConsumers.foreach(_.stop())

  println("DONE")
}