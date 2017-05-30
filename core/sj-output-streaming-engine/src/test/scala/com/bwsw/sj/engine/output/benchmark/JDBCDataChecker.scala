package com.bwsw.sj.engine.output.benchmark

import com.bwsw.sj.common.dal.model.stream.{JDBCStreamDomain, StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.GenericMongoRepository
import com.bwsw.sj.engine.output.benchmark.DataFactory._

import scala.collection.mutable.ArrayBuffer

object JDBCDataChecker extends App {

  val streamService: GenericMongoRepository[StreamDomain] = connectionRepository.getStreamRepository
  val tStream: TStreamStreamDomain = streamService.get(tstreamInputName).get.asInstanceOf[TStreamStreamDomain]
  val inputConsumer = createConsumer(tStream)
  inputConsumer.start()

  val inputElements = new ArrayBuffer[(Int, String)]()
  val partitions = inputConsumer.getPartitions.toIterator

  while (partitions.hasNext) {
    val currentPartition = partitions.next
    var maybeTxn = inputConsumer.getTransaction(currentPartition)
    while (maybeTxn.isDefined) {
      val transaction = maybeTxn.get
      while (transaction.hasNext) {
        val element = objectSerializer.deserialize(transaction.next).asInstanceOf[(Int, String)]
        inputElements.append(element)
      }
      maybeTxn = inputConsumer.getTransaction(currentPartition)
    }
  }

  val jdbcStream: JDBCStreamDomain = streamService.get(jdbcStreamName).get.asInstanceOf[JDBCStreamDomain]

  val jdbcClient = openJdbcConnection(jdbcStream)
  jdbcClient.start()

  val stmt = jdbcClient.createPreparedStatement(s"SELECT COUNT(1) FROM ${jdbcStream.name}")
  var jdbcOutputDataSize = 0
  val res = stmt.executeQuery()
  while (res.next()) {
    jdbcOutputDataSize = res.getInt(1)
  }

  assert(inputElements.size == jdbcOutputDataSize,
    s"Count of all txns elements that are consumed from output stream ($jdbcOutputDataSize) " +
      s"should equals count of all txns elements that are consumed from input stream (${inputElements.size})")

  connectionRepository.close()
  inputConsumer.stop()
  jdbcClient.close()

  println("DONE")
}

