package com.bwsw.sj.engine.output

import java.net.InetSocketAddress

import com.aerospike.client.Host
import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.DAL.model.{SjStream, TStreamService, TStreamSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.tstreams.agents.producer
import com.bwsw.tstreams.common.{CassandraConnectorConf, RoundRobinPolicy}
import com.bwsw.tstreams.converter.IConverter
import com.bwsw.tstreams.coordination.client.TcpTransport
import com.bwsw.tstreams.data.aerospike
import com.bwsw.tstreams.generator.LocalTimeUUIDGenerator
import com.bwsw.tstreams.metadata.{MetadataStorage, MetadataStorageFactory}
import com.bwsw.tstreams.services.BasicStreamService
import com.bwsw.tstreams.streams.TStream

/**
 * @author Kseniya Tomskikh
 */
object OutputTestDataFactory {

  private val streamDAO: GenericMongoService[SjStream] = ConnectionRepository.getStreamService
  val stream: TStreamSjStream = streamDAO.get("s10").asInstanceOf[TStreamSjStream]
  private val objectSerializer = new ObjectSerializer()

  val inputStreamService = stream.service.asInstanceOf[TStreamService]

  private val metadataStorageFactory = new MetadataStorageFactory
  private val cassandraConnectorConf = CassandraConnectorConf.apply(inputStreamService.metadataProvider.hosts.map { addr =>
    val parts = addr.split(":")
    new InetSocketAddress(parts(0), parts(1).toInt)
  }.toSet)
  val metadataStorage: MetadataStorage = metadataStorageFactory.getInstance(cassandraConnectorConf, inputStreamService.metadataNamespace)

  private val dataStorageFactory = new aerospike.Factory
  private val dataStorageHosts = inputStreamService.dataProvider.hosts.map { addr =>
    val parts = addr.split(":")
    new Host(parts(0), parts(1).toInt)
  }.toSet
  private val options = new aerospike.Options(inputStreamService.dataNamespace, dataStorageHosts)
  val dataStorage = dataStorageFactory.getInstance(options)

  private val converter = new IConverter[Array[Byte], Array[Byte]] {
    override def convert(obj: Array[Byte]): Array[Byte] = obj
  }

  def main(args: Array[String]) = {
    createTstreamData(10, 5)
    println("Ok")
  }

  private def createTstreamData(countTxns: Int, countElements: Int) = {
    val _producer = createProducer()
    var number = 0
    val s = System.nanoTime
    (0 until countTxns) foreach { (x: Int) =>
      val txn = _producer.newTransaction(producer.NewTransactionProducerPolicy.ErrorIfOpened)
      (0 until countElements) foreach { (y: Int) =>
        number += 1
        txn.send(objectSerializer.serialize(number.asInstanceOf[Object]))
      }
      txn.checkpoint()
    }

    println(s"producer time: ${(System.nanoTime - s) / 1000000}")

    _producer.stop()
  }

  private def createProducer() = {
    val tStream: TStream[Array[Byte]] =
      BasicStreamService.loadStream(stream.name, metadataStorage, dataStorage)

    val coordinationSettings = new producer.CoordinationOptions(
      zkHosts = List(new InetSocketAddress("localhost", 2181)),
      zkRootPath = "/unit",
      zkConnectionTimeout = 7000,
      zkSessionTimeout = 7000,
      isLowPriorityToBeMaster = false,
      transport = new TcpTransport("localhost:8030", 60 * 1000),
      threadPoolAmount = 1,
      threadPoolPublisherThreadsAmount = 1,
      partitionRedistributionDelay = 2,
      isMasterBootstrapModeFull = true,
      isMasterProcessVote = true)

    val roundRobinPolicy = new RoundRobinPolicy(tStream, (0 until stream.partitions).toList)

    val timeUuidGenerator = new LocalTimeUUIDGenerator

    val options = new producer.Options[Array[Byte]](
      transactionTTL = 6,
      transactionKeepAliveInterval = 2,
      roundRobinPolicy,
      5,
      timeUuidGenerator,
      coordinationSettings,
      converter)

    new producer.Producer[Array[Byte]]("producer for " + tStream.name, tStream, options)
  }


}
