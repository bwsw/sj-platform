package com.bwsw.sj.engine.output

import java.net.InetSocketAddress

import com.aerospike.client.Host
import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.DAL.model.{TStreamSjStream, TStreamService, SjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.tstreams.agents.producer.DataInsertType.BatchInsert
import com.bwsw.tstreams.agents.producer._
import com.bwsw.tstreams.common.CassandraConnectorConf
import com.bwsw.tstreams.converter.IConverter
import com.bwsw.tstreams.coordination.producer.transport.impl.TcpTransport
import com.bwsw.tstreams.data.aerospike.{AerospikeStorage, AerospikeStorageOptions, AerospikeStorageFactory}
import com.bwsw.tstreams.generator.LocalTimeUUIDGenerator
import com.bwsw.tstreams.metadata.{MetadataStorage, MetadataStorageFactory}
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.services.BasicStreamService
import com.bwsw.tstreams.streams.TStream

/**
 * Created: 5/31/16
 *
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

  private val dataStorageFactory = new AerospikeStorageFactory
  private val dataStorageHosts = inputStreamService.dataProvider.hosts.map { addr =>
    val parts = addr.split(":")
    new Host(parts(0), parts(1).toInt)
  }.toList
  private val options = new AerospikeStorageOptions(inputStreamService.dataNamespace, dataStorageHosts)
  val dataStorage: AerospikeStorage = dataStorageFactory.getInstance(options)

  private val converter = new IConverter[Array[Byte], Array[Byte]] {
    override def convert(obj: Array[Byte]): Array[Byte] = obj
  }

  def main(args: Array[String]) = {
    createTstreamData(10, 5)
    println("Ok")
  }

  private def createTstreamData(countTxns: Int, countElements: Int) = {
    val producer = createProducer()
    var number = 0
    val s = System.nanoTime
    (0 until countTxns) foreach { (x: Int) =>
      val txn = producer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened)
      (0 until countElements) foreach { (y: Int) =>
        number += 1
        txn.send(objectSerializer.serialize(number.asInstanceOf[Object]))
      }
      txn.checkpoint()
    }

    println(s"producer time: ${(System.nanoTime - s) / 1000000}")

    producer.stop()
  }

  private def createProducer() = {
    val tStream: TStream[Array[Byte]] =
      BasicStreamService.loadStream(stream.name, metadataStorage, dataStorage)

    val coordinationSettings = new CoordinationOptions(
      agentAddress = s"localhost:8030",
      zkHosts = List(new InetSocketAddress("localhost", 2181)),
      zkRootPath = "/unit",
      zkSessionTimeout = 7000,
      isLowPriorityToBeMaster = false,
      transport = new TcpTransport,
      transportTimeout = 5,
      zkConnectionTimeout = 7000)

    val roundRobinPolicy = new RoundRobinPolicy(tStream, (0 until stream.partitions).toList)

    val timeUuidGenerator = new LocalTimeUUIDGenerator

    val options = new Options[Array[Byte]](
      transactionTTL = 6,
      transactionKeepAliveInterval = 2,
      roundRobinPolicy,
      BatchInsert(5),
      timeUuidGenerator,
      coordinationSettings,
      converter)

    new Producer[Array[Byte]]("producer for " + tStream.name, tStream, options)
  }


}
