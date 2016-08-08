package com.bwsw.sj.engine.input

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.input.DataFactory._

object DuplicateChecker extends App {

  val streamService = ConnectionRepository.getStreamService

  val outputConsumers = (1 to outputCount).map(x => createOutputConsumer(streamService, x.toString))
  outputConsumers.foreach(x => x.start())

  var totalInputElements = args(0).toInt * outputCount
  val totalDuplicates = args(1).toInt * outputCount
  var totalOutputElements = 0

  outputConsumers.foreach(outputConsumer => {
    var maybeTxn = outputConsumer.getTransaction

    while (maybeTxn.isDefined) {
      val txn = maybeTxn.get
      while (txn.hasNext()) {
        txn.next()
        totalOutputElements += 1
      }
      maybeTxn = outputConsumer.getTransaction
    }
  })

  assert(totalOutputElements == totalInputElements - totalDuplicates,
    "Count of all txns elements that are consumed from output stream should equals count of the elements that are consumed from input socket excluding the duplicates")

  outputConsumers.foreach(x => x.stop())
  close()
  ConnectionRepository.close()

  println("DONE")
}
