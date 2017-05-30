package com.bwsw.sj.engine.output.benchmark

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.service.RestServiceDomain
import com.bwsw.sj.common.dal.model.stream.{RestStreamDomain, StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.GenericMongoRepository
import com.bwsw.sj.engine.output.benchmark.DataFactory._
import com.bwsw.sj.engine.output.benchmark.OutputTestRestServer.Entity
import org.eclipse.jetty.client.HttpClient

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

object RestDataChecker extends App {

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

  val restStream: RestStreamDomain = streamService.get(restStreamName).get.asInstanceOf[RestStreamDomain]

  val jsonSerializer = new JsonSerializer()

  val hosts = restStream.service.asInstanceOf[RestServiceDomain].provider.hosts
  val urls = hosts.map("http://" + _)
  var outputElements = Seq[(Int, String)]()
  val client = new HttpClient()
  client.start()
  urls.foreach { url =>
    Try {
      val response = client.GET(url)
      val data = response.getContentAsString
      val list = jsonSerializer.deserialize[Iterable[Entity]](data)
      outputElements = list.map(e => (e.value, e.stringValue)).toSeq
    }
  }
  client.stop()

  assert(inputElements.size == outputElements.size,
    s"Count of all txns elements that are consumed from output stream (${outputElements.size}) " +
      s"should equals count of all txns elements that are consumed from input stream (${inputElements.size})")

  assert(inputElements.forall(x => outputElements.contains(x)) && outputElements.forall(x => inputElements.contains(x)),
    "All txns elements that are consumed from output stream should equals all txns elements that are consumed from input stream")


  connectionRepository.close()
  inputConsumer.stop()

  println("DONE")
}

