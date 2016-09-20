package com.bwsw.sj.engine.output.benchmark

import com.bwsw.sj.common.DAL.model.{ESSjStream, SjStream, TStreamSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.engine.output.benchmark.BenchmarkDataFactory._

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

/**
  *
  *
  * @author Kseniya Tomskikh
  */
object OutputModuleDataChecker extends App {

  val streamService: GenericMongoService[SjStream] = ConnectionRepository.getStreamService
  val tStream: TStreamSjStream = streamService.get(tStreamName).asInstanceOf[TStreamSjStream]
  val inputConsumer = createConsumer(tStream)

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

  val esStream: ESSjStream = streamService.get(esStreamName).asInstanceOf[ESSjStream]

  val (esClient, esService) = openDbConnection(esStream)

  val outputData = esClient.search(esService.index, esStream.name)

  val outputElements = new ArrayBuffer[Int]()
  outputData.getHits.foreach { hit =>
    val content = hit.getSource.asScala
    val value = content.get("value").get
    outputElements.append(value.asInstanceOf[Int])
  }

  assert(inputElements.size == outputElements.size,
    "Count of all txns elements that are consumed from output stream should equals count of all txns elements that are consumed from input stream")

  assert(inputElements.forall(x => outputElements.contains(x)) && outputElements.forall(x => inputElements.contains(x)),
    "All txns elements that are consumed from output stream should equals all txns elements that are consumed from input stream")

  esClient.close()
  ConnectionRepository.close()

  println("DONE")

}
