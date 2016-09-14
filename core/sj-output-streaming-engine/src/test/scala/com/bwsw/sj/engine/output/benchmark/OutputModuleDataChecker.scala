package com.bwsw.sj.engine.output.benchmark

import java.net.InetSocketAddress

import com.aerospike.client.Host
import com.bwsw.sj.common.DAL.model.{ESSjStream, SjStream, TStreamService, TStreamSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.engine.output.benchmark.BenchmarkDataFactory._
import com.bwsw.tstreams.common.CassandraConnectorConf
import com.bwsw.tstreams.data.aerospike
import com.bwsw.tstreams.metadata.{MetadataStorage, MetadataStorageFactory}

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

  val tStreamService = tStream.service.asInstanceOf[TStreamService]
  val metadataStorageFactory = new MetadataStorageFactory
  val cassandraConnectorConf = CassandraConnectorConf.apply(tStreamService.metadataProvider.hosts.map { addr =>
    val parts = addr.split(":")
    new InetSocketAddress(parts(0), parts(1).toInt)
  }.toSet)
  val metadataStorage: MetadataStorage = metadataStorageFactory.getInstance(cassandraConnectorConf, tStreamService.metadataNamespace)

  val dataStorageFactory = new aerospike.Factory
  val dataStorageHosts = tStreamService.dataProvider.hosts.map {addr =>
    val parts = addr.split(":")
    new Host(parts(0), parts(1).toInt)
  }.toSet
  val aerospikeOptions = new aerospike.Options(tStreamService.dataNamespace, dataStorageHosts)
  val dataStorage = dataStorageFactory.getInstance(aerospikeOptions)

  val inputConsumer = createConsumer(tStream, "localhost:8188", metadataStorage, dataStorage)

  val inputElements = new ArrayBuffer[Int]()
  val partitions = inputConsumer.getPartitions().toIterator

  while (partitions.hasNext) {
    val currentPartition = partitions.next()
    var maybeTxn = inputConsumer.getTransaction(currentPartition)
    while (maybeTxn.isDefined) {
      val txn = maybeTxn.get
      while (txn.hasNext()) {
        val element = objectSerializer.deserialize(txn.next()).asInstanceOf[Int]
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

  metadataStorageFactory.closeFactory()
  dataStorageFactory.closeFactory()
  esClient.close()
  ConnectionRepository.close()

  println("DONE")

}
