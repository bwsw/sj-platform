package com.bwsw.sj.engine.output

import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock

import com.aerospike.client.Host
import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.tstreams.agents.consumer.Offsets.Oldest
import com.bwsw.tstreams.agents.consumer.subscriber.{BasicSubscriberCallback, BasicSubscribingConsumer}
import com.bwsw.tstreams.agents.consumer.{BasicConsumerTransaction, ConsumerCoordinationSettings, BasicConsumerOptions, BasicConsumer}
import com.bwsw.tstreams.converter.{IConverter, ArrayByteToStringConverter}
import com.bwsw.tstreams.data.aerospike.{AerospikeStorageOptions, AerospikeStorageFactory}
import com.bwsw.tstreams.generator.LocalTimeUUIDGenerator
import com.bwsw.tstreams.metadata.MetadataStorageFactory
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.services.BasicStreamService
import com.bwsw.sj.common.ModuleConstants._

/**
  * Created: 26/05/2016
  *
  * @author Kseniya Tomskikh
  */
object OutputTaskRunner {

  val streamDAO = ConnectionRepository.getStreamService
  val objectSerializer = new ObjectSerializer()

  val metadataStorageFactory = new MetadataStorageFactory
  val metadataStorage = metadataStorageFactory.getInstance(
    cassandraHosts = List(new InetSocketAddress("localhost", 9042)),
    keyspace = "test")

  val aerospikeStorageFactory = new AerospikeStorageFactory
  val hosts = List(
    new Host("localhost",3000),
    new Host("localhost",3001),
    new Host("localhost",3002),
    new Host("localhost",3003))
  val aerospikeOptions = new AerospikeStorageOptions("test", hosts)
  val aerospikeStorage = aerospikeStorageFactory.getInstance(aerospikeOptions)

  val arrayByteToStringConverter = new ArrayByteToStringConverter

  val converter = new IConverter[Array[Byte], Array[Byte]] {
    override def convert(obj: Array[Byte]): Array[Byte] = {
      obj
    }
  }

  def main(args: Array[String]) = {

    val inputSream: SjStream = streamDAO.get("s1")

    val stream = BasicStreamService.loadStream("s1", metadataStorage, aerospikeStorage)
    val roundRobinPolicy = new RoundRobinPolicy(stream, (0 until inputSream.partitions).toList)
    val timeUuidGenerator = new LocalTimeUUIDGenerator
    val consumerOptions = new BasicConsumerOptions[Array[Byte], Array[Byte]](
      transactionsPreload = 10,
      dataPreload = 7,
      consumerKeepAliveInterval = 5,
      converter,
      roundRobinPolicy,
      new ConsumerCoordinationSettings("localhost:8588", "/unit", List(new InetSocketAddress("localhost", 2181)), 7000),
      Oldest,
      timeUuidGenerator,
      useLastOffset = true)

    val lock = new ReentrantLock()
    var acc = 0
    val callback = new BasicSubscriberCallback[Array[Byte], Array[Byte]] {
      override def onEvent(subscriber : BasicSubscribingConsumer[Array[Byte], Array[Byte]], partition: Int, transactionUuid: UUID): Unit = {
        lock.lock()
        acc += 1
        lock.unlock()
      }
      override val frequency: Int = 1
    }

    val subscribeConsumer = new BasicSubscribingConsumer[Array[Byte], Array[Byte]](
      "test_cons",
      stream,
      consumerOptions,
      callback,
      persistentQueuePath
    )

    val consumer = new BasicConsumer[Array[Byte], Array[Byte]](
      stream.name,
      stream,
      consumerOptions)

    subscribeConsumer.start()

    var totalInputElements = 0
    var inputElements = scala.collection.mutable.ArrayBuffer[Int]()

    var nextTxn: Option[BasicConsumerTransaction[Array[Byte], Array[Byte]]] = consumer.getTransaction
    while (nextTxn.isDefined) {
      val txn = nextTxn.get
      while (txn.hasNext()) {
        val element = objectSerializer.deserialize(txn.next()).asInstanceOf[Int]
        inputElements.+=(element)
        totalInputElements += 1
      }
      nextTxn = consumer.getTransaction
    }

  }

}
