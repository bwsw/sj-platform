package com.bwsw.sj.crud.rest

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.crud.rest.entities.SjStreamData
import com.datastax.driver.core.{Cluster, Session}

/**
  * Created: 4/14/16
  *
  * @author Kseniya Tomskikh
  */
object SjTest {

  val serializer = new JsonSerializer

  private val instanceJson = "{\n  " +
    "\"uuid\" : \"qwe-123-dsf\",\n  " +
    "\"name\" : \"instance-test\",\n  " +
    "\"description\" : \"\",\n  " +
    "\"inputs\" : [\"s1/split\", \"s2/full\", \"s3/split\"],\n  " +
    "\"outputs\" : [\"s2\", \"s3\"],\n  " +
    "\"checkpoint-mode\" : \"time-interval\",\n  " +
    "\"checkpoint-interval\" : 100,\n  " +
    "\"state-management\" : \"ram\",\n  " +
    "\"state-full-checkpoint\" : 5,\n  " +
    "\"parallelism\" : 10,\n  " +
    "\"options\" : {\"11\" : \"3e2ew\"},\n  " +
    "\"start-from\" : \"oldest\",\n  " +
    "\"per-task-cores\" : 2,\n  " +
    "\"per-task-ram\" : 128,\n  " +
    "\"jvm-options\" : {\"a\" : \"dsf\"}\n}"

  private val spec = "{\n  \"name\": \"com.bwsw.sj.stub\",\n  " +
    "\"description\": \"Stub module by BW\",\n  " +
    "\"version\": \"0.1\",\n  " +
    "\"author\": \"Ksenia Mikhaleva\",\n  " +
    "\"license\": \"Apache 2.0\",\n  " +
    "\"inputs\": {\n    \"cardinality\": [\n      1,\n      1\n    ],\n    \"types\": [\n      \"stream.t-stream\"\n    ]\n  },\n  " +
    "\"outputs\": {\n    \"cardinality\": [\n      1,\n      1\n    ],\n    \"types\": [\n      \"stream.t-stream\"\n    ]\n  },\n  " +
    "\"module-type\": \"regular-streaming\",\n  " +
    "\"engine\": \"streaming\",\n  " +
    "\"options\": {\n    \"opt\": 1\n  },\n  " +
    "\"validator-class\": \"com.bwsw.sj.stub.Validator\",\n  " +
    "\"executor-class\": \"com.bwsw.sj.stub.Executor\"\n}"

  serializer.setIgnoreUnknown(true)


  val cassStreamJson = "{\n\t\"name\" : \"cass\",\n\t\"description\" : \"dasgf\",\n\t\"stream-type\" : \"cassandra\",\n\t\"keyspace\" : \"testing\"\n}"
  val testJson = "{\n\t\"name\" : \"tst\",\n\t\"description\" : \"fdsgff\",\n\t\"stream-type\" : \"test\",\n\t\"ttt\" : 26\n}"

  def main(args: Array[String]) = {
    //createData()
    //prepareCassandra()
    /*val dao = ConnectionRepository.getStreamService
    val streams = dao.getAll
    val stream = streams.filter(s => s.name.equals("s1")).head
    println(stream.service.name)
    println(ConnectionRepository.getServiceManager.get(stream.service.name).asInstanceOf[TStreamService].dataProvider.name)*/

    /*val cass = serializer.deserialize[SjStreamTest](cassStreamJson)
    println(cass.getClass.toString)
    val test = serializer.deserialize[SjStreamTest](testJson)
    println(test.getClass.toString)*/
    //createKafkaData()
    println("Ok")
  }

  def prepareCassandra() = {
    val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()
    val session = cluster.connect()
    createKeyspace(session, "test_keyspace")
    createMetadataTables(session, "test_keyspace")
    createDataTable(session, "test_keyspace")
  }

  def createData() = {
    val serviceDAO: GenericMongoService[Service] = ConnectionRepository.getServiceManager
    val providerDAO = ConnectionRepository.getProviderService
    createProviders(providerDAO)
    createServices(serviceDAO, providerDAO)
    val tService = serviceDAO.get("tstrq_service")
    createStreams(tService, serviceDAO.get("zk_service").asInstanceOf[ZKService])
  }

  def createKafkaData() = {
    val providerDAO = ConnectionRepository.getProviderService
    val serviceDAO = ConnectionRepository.getServiceManager
    val streamDAO = ConnectionRepository.getStreamService

    val provider = new Provider()
    provider.name = "kafka"
    provider.hosts = Array("192.168.1.180:9092")
    provider.providerType = "kafka"
    providerDAO.save(provider)

    val service = new KafkaService()
    service.provider = provider
    service.name = "kafka_service"
    service.serviceType = "KfkQ"
    serviceDAO.save(service)

    val stream = new SjStream()
    stream.name = "s5"
    stream.generator = null
    stream.partitions = 3
    stream.service = service
    stream.streamType = "kafka"
    stream.tags = "test"
    streamDAO.save(stream)
  }

  def createServices(serviceDAO: GenericMongoService[Service], providerDAO: GenericMongoService[Provider]) = {
    val cassService = new CassandraService
    val cassProv = providerDAO.get("cass_prov")
    cassService.keyspace = "test_keyspace"
    cassService.name = "cass_service"
    cassService.description = "cassandra test service"
    cassService.provider = cassProv
    cassService.serviceType = "CassDB"
    serviceDAO.save(cassService)

    val zkService = new ZKService
    val zkProv = providerDAO.get("zk_prov")
    zkService.namespace = "zk_test"
    zkService.name = "zk_service"
    zkService.description = "zookeeper test service"
    zkService.provider = zkProv
    zkService.serviceType = "ZKCoord"
    serviceDAO.save(zkService)

    val aeroService = new AerospikeService
    val aeroProv = providerDAO.get("aero_prov")
    aeroService.namespace = "test"
    aeroService.name = "aero_service"
    aeroService.description = "aerospike test service"
    aeroService.provider = aeroProv
    aeroService.serviceType = "ArspkDB"
    serviceDAO.save(aeroService)

    val redisService = new RedisService
    val redisProv = providerDAO.get("redis_prov")
    redisService.namespace = "test"
    redisService.name = "rd_service"
    redisService.description = "redis test service"
    redisService.provider = redisProv
    redisService.serviceType = "RdsCoord"
    serviceDAO.save(redisService)

    val tstrqService = new TStreamService
    tstrqService.name = "tstrq_service"
    tstrqService.metadataProvider = cassProv
    tstrqService.metadataNamespace = "test_keyspace"
    tstrqService.dataProvider = aeroProv
    tstrqService.dataNamespace = "test"
    tstrqService.lockProvider = redisProv
    tstrqService.lockNamespace = "test"
    serviceDAO.save(tstrqService)
  }

  def createProviders(providerDAO: GenericMongoService[Provider]) = {
    val cassProv = new Provider
    cassProv.providerType = "cassandra"
    cassProv.name = "cass_prov"
    cassProv.login = ""
    cassProv.password = ""
    cassProv.description = "cassandra provider test"
    cassProv.hosts = Array("127.0.0.1:9042")
    providerDAO.save(cassProv)

    val aeroProv = new Provider
    aeroProv.providerType = "aerospike"
    aeroProv.name = "aero_prov"
    aeroProv.login = ""
    aeroProv.password = ""
    aeroProv.description = "aerospike provider test"
    aeroProv.hosts = Array("127.0.0.1:3000", "127.0.0.1:3001")
    providerDAO.save(aeroProv)

    val redisProv = new Provider
    redisProv.providerType = "redis"
    redisProv.name = "redis_prov"
    redisProv.login = ""
    redisProv.password = ""
    redisProv.description = "redis provider test"
    redisProv.hosts = Array("127.0.0.1:6379")
    providerDAO.save(redisProv)

    val zkProv = new Provider
    zkProv.providerType = "zookeeper"
    zkProv.name = "zk_prov"
    zkProv.login = ""
    zkProv.password = ""
    zkProv.description = "zookeeper provider test"
    zkProv.hosts = Array("176.120.25.19:2181")
    providerDAO.save(zkProv)
  }

  def createStreams(tService: Service, service: ZKService) = {
    val generator1 = new Generator
    generator1.generatorType = "global"
    generator1.instanceCount = 1
    generator1.service = service

    val sjStreamDAO = ConnectionRepository.getStreamService
    val s1 = new SjStream
    s1.name = "s1"
    s1.description = "s1 stream"
    s1.partitions = 7
    s1.service = tService
    s1.streamType = "Tstream"
    s1.tags = "TAG"
    s1.generator = generator1
    sjStreamDAO.save(s1)

    val generator2 = new Generator
    generator2.generatorType = "global"
    generator2.instanceCount = 2
    generator2.service = service
    val s2 = new SjStream
    s2.name = "s2"
    s2.description = "s2 stream"
    s2.partitions = 10
    s2.service = tService
    s2.streamType = "Tstream"
    s2.tags = "TAG"
    s2.generator = generator2
    sjStreamDAO.save(s2)

    val generator3 = new Generator
    generator3.generatorType = "global"
    generator3.instanceCount = 3
    generator3.service = service
    val s3 = new SjStream
    s3.name = "s3"
    s3.description = "s3 stream"
    s3.partitions = 10
    s3.service = tService
    s3.streamType = "Tstream"
    s3.tags = "TAG"
    s3.generator = generator3
    sjStreamDAO.save(s3)
  }

  /**
    * Keyspace creator helper
 *
    * @param session Session instance which will be used for keyspace creation
    * @param keyspace Keyspace name
    */
  def createKeyspace(session: Session, keyspace: String) = session.execute(s"CREATE KEYSPACE $keyspace WITH replication = " +
    s" {'class': 'SimpleStrategy', 'replication_factor': '1'} " +
    s" AND durable_writes = true")

  /**
    * Metadata tables creator helper
 *
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
 *
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
 *
    * @param session Session
    * @param keyspace Keyspace name
    */
  def dropDataTable(session: Session, keyspace: String) = {
    session.execute(s"DROP TABLE $keyspace.data_queue")
  }

  /**
    * Cassandra metadata storage table dropper helper
 *
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
 *
    * @param session  Session
    * @param keyspace Keyspace name
    */
  def clearMetadataTables(session: Session, keyspace: String) = {
    dropMetadataTables(session, keyspace)
    createMetadataTables(session, keyspace)
  }


  /**
    * Cassandra data table creator helper
 *
    * @param session Session
    * @param keyspace Keyspace name
    */
  def clearDataTable(session: Session, keyspace: String) = {
    dropDataTable(session, keyspace)
    createDataTable(session, keyspace)
  }


}
