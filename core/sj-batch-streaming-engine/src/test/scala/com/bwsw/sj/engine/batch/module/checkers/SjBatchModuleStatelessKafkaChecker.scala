package com.bwsw.sj.engine.batch.module.checkers

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.engine.core.entities.{KafkaEnvelope, TStreamEnvelope, Batch}
import com.bwsw.sj.engine.batch.module.DataFactory._
import scala.collection.JavaConverters._

object SjBatchModuleStatelessKafkaChecker extends App {
  val streamService = ConnectionRepository.getStreamService
  val objectSerializer = new ObjectSerializer()

  val inputConsumer = createInputKafkaConsumer(inputCount, partitions)
  val outputConsumers = (1 to outputCount).map(x => createOutputConsumer(partitions, x.toString))

  outputConsumers.foreach(x => x.start())

  var totalInputElements = 0
  var totalOutputElements = 0

  var inputElements = scala.collection.mutable.ArrayBuffer[Int]()
  var outputElements = scala.collection.mutable.ArrayBuffer[Int]()

  var records = inputConsumer.poll(1000 * 20)

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
            case tstreamEnvelope: TStreamEnvelope[Int @unchecked] => tstreamEnvelope.data.foreach(x => {
              outputElements.+=(x)
              totalOutputElements += 1
            })
            case kafkaEnvelope: KafkaEnvelope[Int @unchecked] =>
              outputElements.+=(kafkaEnvelope.data)
              totalOutputElements += 1
          }
        }
        maybeTxn = outputConsumer.getTransaction(currentPartition)
      }
    }
  })

  assert(totalInputElements == totalOutputElements,
    "Count of all txns elements that are consumed from output stream should equals count of all txns elements that are consumed from input stream")

  assert(inputElements.forall(x => outputElements.contains(x)) && outputElements.forall(x => inputElements.contains(x)),
    "All txns elements that are consumed from output stream should equals all txns elements that are consumed from input stream")

  outputConsumers.foreach(x => x.stop())
  ConnectionRepository.close()

  println("DONE")
}
