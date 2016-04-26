package utils


import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock

import com.aerospike.client.Host
import com.bwsw.sj.common.module.PersistentBlockingQueue
import com.bwsw.tstreams.agents.consumer.Offsets.Oldest
import com.bwsw.tstreams.agents.consumer.{BasicConsumerCallback, BasicConsumerOptions, BasicConsumerWithSubscribe}
import com.bwsw.tstreams.agents.producer.InsertionType.BatchInsert
import com.bwsw.tstreams.agents.producer.{BasicProducer, BasicProducerOptions, ProducerPolicies}
import com.bwsw.tstreams.converter.IConverter
import com.bwsw.tstreams.coordination.Coordinator
import com.bwsw.tstreams.data.aerospike.{AerospikeStorageFactory, AerospikeStorageOptions}
import com.bwsw.tstreams.generator.LocalTimeUuidGenerator
import com.bwsw.tstreams.metadata.MetadataStorageFactory
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.streams.BasicStream
import com.datastax.driver.core.Session
import org.redisson.{Config, Redisson}

object helper {
  /**
   * Keyspace creator helper
   * @param session Session instance which will be used for keyspace creation
   * @param keyspace Keyspace name
   */
  def createKeyspace(session: Session, keyspace: String) = session.execute(s"CREATE KEYSPACE $keyspace WITH replication = " +
    s" {'class': 'SimpleStrategy', 'replication_factor': '1'} " +
    s" AND durable_writes = true")

  /**
   * Metadata tables creator helper
   * @param session Session
   * @param keyspace Keyspace name
   */
  def createMetadataTables(session: Session, keyspace: String) = {

    session.execute(s"CREATE TABLE $keyspace.stream_commit_last (" +
      s"stream text, " +
      s"partition int, " +
      s"transaction timeuuid, " +
      s"PRIMARY KEY (stream, partition))")

    session.execute(s"CREATE TABLE $keyspace.consumers (" +
      s"name text, " +
      s"stream text, " +
      s"partition int, " +
      s"last_transaction timeuuid, " +
      s"PRIMARY KEY (name, stream, partition))")


    session.execute(s"CREATE TABLE $keyspace.streams (" +
      s"stream_name text PRIMARY KEY, " +
      s"partitions int," +
      s"ttl int, " +
      s"description text)")


    session.execute(s"CREATE TABLE $keyspace.commit_log (" +
      s"stream text, " +
      s"partition int, " +
      s"transaction timeuuid, " +
      s"cnt int, " +
      s"PRIMARY KEY (stream, partition, transaction))")


    session.execute(s"CREATE TABLE $keyspace.generators (" +
      s"name text, " +
      s"time timeuuid, " +
      s"PRIMARY KEY (name))")

  }

  /**
   * Cassandra data table creator helper
   * @param session Session
   * @param keyspace Keyspace name
   */
  def createDataTable(session: Session, keyspace: String) = {

    session.execute(s"CREATE TABLE $keyspace.data_queue ( " +
      s"stream text, " +
      s"partition int, " +
      s"transaction timeuuid, " +
      s"seq int, " +
      s"data blob, " +
      s"PRIMARY KEY ((stream, partition), transaction, seq))")
  }

  /**
   * Cassandra storage table dropper helper
   * @param session Session
   * @param keyspace Keyspace name
   */
  def dropDataTable(session: Session, keyspace: String) = {
    session.execute(s"DROP TABLE $keyspace.data_queue")
  }

  /**
   * Cassandra metadata storage table dropper helper
   * @param session Session
   * @param keyspace Keyspace name
   */
  def dropMetadataTables(session: Session, keyspace: String) = {
    session.execute(s"DROP TABLE $keyspace.stream_commit_last")

    session.execute(s"DROP TABLE $keyspace.consumers")

    session.execute(s"DROP TABLE $keyspace.streams")

    session.execute(s"DROP TABLE $keyspace.commit_log")

    session.execute(s"DROP TABLE $keyspace.generators")
  }

  /**
   * Metadata table flushing helper
   * @param session Session
   * @param keyspace Keyspace name
   */
  def clearMetadataTables(session: Session, keyspace: String) = {
    dropMetadataTables(session, keyspace)
    createMetadataTables(session, keyspace)
  }


  /**
   * Cassandra data table creator helper
   * @param session Session
   * @param keyspace Keyspace name
   */
  def clearDataTable(session: Session, keyspace: String) = {
    dropDataTable(session, keyspace)
    createDataTable(session, keyspace)
  }
}

object asd {
  def main(args: Array[String]) {
    val randomKeyspace = "random1"
//    val cluster = Cluster.builder().addContactPoint("localhost").build()
//    val session = cluster.connect()
//    helper.createKeyspace(session, randomKeyspace)
//    helper.createMetadataTables(session, randomKeyspace)
//    helper.createDataTable(session, randomKeyspace)

    //metadata/data factories
    val metadataStorageFactory = new MetadataStorageFactory
    val storageFactory = new AerospikeStorageFactory

    //converters to convert usertype->storagetype; storagetype->usertype
    val converter = new IConverter[Array[Byte],Array[Byte]] {
      override def convert(obj: Array[Byte]): Array[Byte] = obj
    }

    //aerospike storage instances
    val hosts = List(
      new Host("localhost", 3000),
      new Host("localhost", 3001),
      new Host("localhost", 3002),
      new Host("localhost", 3003))
    val aerospikeOptions = new AerospikeStorageOptions("test", hosts)
    val aerospikeStorage = storageFactory.getInstance(aerospikeOptions)

    //metadata storage instances
    val metadataStorage = metadataStorageFactory.getInstance(
      cassandraHosts = List(new InetSocketAddress("localhost", 9042)),
      keyspace = randomKeyspace)

    //coordinator for coordinating producer/consumer
    val config = new Config()
    config.useSingleServer().setAddress("localhost:6379")
    val redissonClient = Redisson.create(config)
    val coordinator = new Coordinator("some_path", redissonClient)

    //stream instances for producer/consumer
    val streamForProducer: BasicStream[Array[Byte]] = new BasicStream[Array[Byte]](
      name = "debug",
      partitions = 3,
      metadataStorage = metadataStorage,
      dataStorage = aerospikeStorage,
      coordinator = coordinator,
      ttl = 60 * 10,
      description = "some_description")

    val streamForConsumer = new BasicStream[Array[Byte]](
      name = "debug",
      partitions = 3,
      metadataStorage = metadataStorage,
      dataStorage = aerospikeStorage,
      coordinator = coordinator,
      ttl = 60 * 10,
      description = "some_description")

    val policyForProducer = new RoundRobinPolicy(streamForProducer, List(0, 1, 2))
    val generatorForProducer = new LocalTimeUuidGenerator
    ///
    val policyForConsumer = new RoundRobinPolicy(streamForConsumer, List(0, 1, 2))
    val generatorForConsumer = new LocalTimeUuidGenerator

    //producer/consumer options
    val producerOptions = new BasicProducerOptions[Array[Byte], Array[Byte]](
      transactionTTL = 6,
      transactionKeepAliveInterval = 2,
      producerKeepAliveInterval = 1,
      policyForProducer,
      BatchInsert(5),
      generatorForProducer,
      converter)

    val consumerOptions = new BasicConsumerOptions[Array[Byte], Array[Byte]](
      transactionsPreload = 10,
      dataPreload = 7,
      consumerKeepAliveInterval = 5,
      converter,
      policyForConsumer,
      Oldest,
      generatorForConsumer,
      useLastOffset = false)


    //val a = BasicStreamService.isExist("s2", metadataStorage)
    val producer = new BasicProducer("name", streamForProducer, producerOptions)

    val lock = new ReentrantLock(true)
    val q = new PersistentBlockingQueue("queue_path")
//    val q = scala.collection.mutable.Queue[String]()
    var cnt = 0

    val callback = new BasicConsumerCallback[Array[Byte], Array[Byte]] {
      override def onEvent(subscriber: BasicConsumerWithSubscribe[Array[Byte], Array[Byte]], partition: Int, transactionUuid: UUID): Unit = {
        lock.lock()
        println(cnt)
        cnt += 1
        lock.unlock()
        q.put(transactionUuid.toString)
      }
      override val frequency: Int = 1
    }

    val consumer = new BasicConsumerWithSubscribe("test9", streamForConsumer, consumerOptions, callback, "path_queue_test")

    (0 until 8000) foreach { x =>
      val txn = producer.newTransaction(ProducerPolicies.errorIfOpen)
      (0 until 100) foreach { _ =>
        val a = Array[Byte]()
        txn.send(a)
      }
      if (x == 1000)
        consumer.start()
      if (x == 4000)
        Thread.sleep(10000)
      if (x == 7997)
        Thread.sleep(60000)

      txn.checkpoint()
      println("sended="+x)
    }


    println("total="+cnt)

    var i = 0
    while(true){
      q.get(8)
      println("receivedpart=" + i)
      i+=1
    }
  }
}
