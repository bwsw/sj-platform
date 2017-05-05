package com.bwsw.sj.engine.input

import com.bwsw.sj.common._dal.repository.ConnectionRepository
import com.bwsw.sj.engine.input.DataFactory._

object DuplicateChecker extends App {

  val streamService = ConnectionRepository.getStreamService

  val outputConsumers = (1 to outputCount).map(x => createOutputConsumer(streamService, x.toString))
  outputConsumers.foreach(x => x.start())

  var totalInputElements = args(0).toInt * outputCount
  val totalDuplicates = args(1).toInt * outputCount
  var totalOutputElements = 0

  outputConsumers.foreach(outputConsumer => {
    val partitions = outputConsumer.getPartitions().toIterator

    while (partitions.hasNext) {
      val currentPartition = partitions.next()
      var maybeTxn = outputConsumer.getTransaction(currentPartition)

      while (maybeTxn.isDefined) {
        val transaction = maybeTxn.get
        while (transaction.hasNext()) {
          transaction.next()
          totalOutputElements += 1
        }
        maybeTxn = outputConsumer.getTransaction(currentPartition)
      }
    }
  })

  assert(totalOutputElements == totalInputElements - totalDuplicates,
    s"Count of all txns elements that are consumed from output stream($totalOutputElements) should equals count of the elements that are consumed from input socket excluding the duplicates(${totalInputElements - totalDuplicates})")

  outputConsumers.foreach(x => x.stop())
  ConnectionRepository.close()

  println("DONE")
}
