package com.bwsw.sj.engine.output.benchmark

import com.bwsw.sj.common.dal.model.stream.{ESStreamDomain, StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.GenericMongoRepository
import com.bwsw.sj.engine.output.benchmark.DataFactory._

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

/**
  *
  *
  * @author Kseniya Tomskikh
  */
object ESDataChecker extends App {

  val streamService: GenericMongoRepository[StreamDomain] = connectionRepository.getStreamRepository
  val tStream: TStreamStreamDomain = streamService.get(tstreamInputName).get.asInstanceOf[TStreamStreamDomain]
  val inputConsumer = createConsumer(tStream)
  inputConsumer.start()

  val inputElements = new ArrayBuffer[Int]()
  val partitions = inputConsumer.getPartitions.toIterator

  while (partitions.hasNext) {
    val currentPartition = partitions.next
    var maybeTxn = inputConsumer.getTransaction(currentPartition)
    while (maybeTxn.isDefined) {
      val transaction = maybeTxn.get
      while (transaction.hasNext) {
        val element = objectSerializer.deserialize(transaction.next()).asInstanceOf[(Int, String)]
        inputElements.append(element._1)
      }
      maybeTxn = inputConsumer.getTransaction(currentPartition)
    }
  }

  val esStream: ESStreamDomain = streamService.get(esStreamName).get.asInstanceOf[ESStreamDomain]

  val (esClient, esService) = openEsConnection(esStream)

  val outputData = esClient.search(esService.index, esStream.name)

  val outputElements = new ArrayBuffer[Int]()
  outputData.getHits.foreach { hit =>
    val content = hit.getSource.asScala
    val value = content("value")
    outputElements.append(value.asInstanceOf[Int])
  }

  assert(inputElements.size == outputElements.size,
    s"Count of all txns elements that are consumed from output stream (${outputElements.size}) " +
      s"should equals count of all txns elements that are consumed from input stream (${inputElements.size})")

  assert(inputElements.forall(x => outputElements.contains(x)) && outputElements.forall(x => inputElements.contains(x)),
    "All txns elements that are consumed from output stream should equals all txns elements that are consumed from input stream")

  esClient.close()
  connectionRepository.close()
  inputConsumer.stop()

  println("DONE")
}
