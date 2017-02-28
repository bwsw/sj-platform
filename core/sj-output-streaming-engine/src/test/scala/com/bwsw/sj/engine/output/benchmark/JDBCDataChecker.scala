package com.bwsw.sj.engine.output.benchmark

import com.bwsw.sj.common.DAL.model.{JDBCSjStream, SjStream, TStreamSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.engine.output.benchmark.DataFactory._

import scala.collection.mutable.ArrayBuffer

object JDBCDataChecker extends App {

  val streamService: GenericMongoService[SjStream] = ConnectionRepository.getStreamService
  val tStream: TStreamSjStream = streamService.get(tstreamInputName).get.asInstanceOf[TStreamSjStream]
  val inputConsumer = createConsumer(tStream)
  inputConsumer.start()

  val inputElements = new ArrayBuffer[Int]()
  val partitions = inputConsumer.getPartitions().toIterator

  while (partitions.hasNext) {
    val currentPartition = partitions.next()
    var maybeTxn = inputConsumer.getTransaction(currentPartition)
    while (maybeTxn.isDefined) {
      val transaction = maybeTxn.get
      while (transaction.hasNext()) {
        val element = objectSerializer.deserialize(transaction.next()).asInstanceOf[Int]
        inputElements.append(element)
      }
      maybeTxn = inputConsumer.getTransaction(currentPartition)
    }
  }

  val jdbcStream: JDBCSjStream = streamService.get(jdbcStreamName).get.asInstanceOf[JDBCSjStream]

  val (jdbcClient, jdbcService) = openJdbcConnection(jdbcStream)

  val stmt = jdbcClient.connection.createStatement()
  var jdbcOutputDataSize = 0
  val res = stmt.executeQuery(s"SELECT COUNT(1) FROM ${jdbcStream.name}")
  while (res.next()) {
    jdbcOutputDataSize = res.getInt(1)
  }

  assert(inputElements.size == jdbcOutputDataSize,
    s"Count of all txns elements that are consumed from output stream ($jdbcOutputDataSize) " +
      s"should equals count of all txns elements that are consumed from input stream (${inputElements.size})")

  ConnectionRepository.close()
  inputConsumer.stop()

  println("DONE")

}

