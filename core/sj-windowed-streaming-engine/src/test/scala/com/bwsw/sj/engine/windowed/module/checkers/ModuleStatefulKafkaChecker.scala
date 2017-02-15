package com.bwsw.sj.engine.windowed.module.checkers

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.entities.{TStreamEnvelope, Batch, KafkaEnvelope}
import com.bwsw.sj.engine.windowed.module.DataFactory._
import com.bwsw.sj.engine.windowed.utils.StateHelper

import scala.collection.JavaConverters._

object ModuleStatefulKafkaChecker extends App {
  open()
  val streamService = ConnectionRepository.getStreamService
  val objectSerializer = new ObjectSerializer()

  val inputConsumer = createInputKafkaConsumer(streamService, partitions)
  val outputConsumers = (1 to outputCount).map(x => createOutputConsumer(streamService, x.toString))

  outputConsumers.foreach(x => x.start())

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
    val partitions = outputConsumer.getPartitions().toIterator

    while (partitions.hasNext) {
      val currentPartition = partitions.next()
      var maybeTxn = outputConsumer.getTransaction(currentPartition)

      while (maybeTxn.isDefined) {
        val transaction = maybeTxn.get
        while (transaction.hasNext()) {
          val batch = objectSerializer.deserialize(transaction.next()).asInstanceOf[Batch]
          batch.envelopes.foreach {
            case tstreamEnvelope: TStreamEnvelope[Array[Byte] @unchecked] => tstreamEnvelope.data.foreach(x => {
              outputElements.+=(objectSerializer.deserialize(x).asInstanceOf[Int])
              totalOutputElements += 1
            })
            case kafkaEnvelope: KafkaEnvelope[Array[Byte] @unchecked] =>
              outputElements.+=(objectSerializer.deserialize(kafkaEnvelope.data).asInstanceOf[Int])
              totalOutputElements += 1
          }
        }
        maybeTxn = outputConsumer.getTransaction(currentPartition)
      }
    }
  })

  val consumer = createStateConsumer(streamService)
  consumer.start()
  val initialState = StateHelper.getState(consumer, objectSerializer)

  assert(totalInputElements == totalOutputElements,
    "Count of all txns elements that are consumed from output stream should equals count of all txns elements that are consumed from input stream")

  assert(inputElements.forall(x => outputElements.contains(x)) && outputElements.forall(x => inputElements.contains(x)),
    "All txns elements that are consumed from output stream should equals all txns elements that are consumed from input stream")

  assert(initialState("sum") == inputElements.sum,
    "Sum of all txns elements that are consumed from input stream should equals state variable sum")

  consumer.stop()
  outputConsumers.foreach(x => x.stop())
  close()
  ConnectionRepository.close()

  println("DONE")
}
