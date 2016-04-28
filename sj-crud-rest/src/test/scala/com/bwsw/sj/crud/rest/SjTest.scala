package com.bwsw.sj.crud.rest

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.{GenericMongoService, ConnectionRepository}
import com.bwsw.sj.common.entities._
import com.mongodb.casbah.MongoClient


import scala.collection.immutable.IndexedSeq
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import com.mongodb.casbah.Imports._

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


  def main(args: Array[String]) = {
    //createData()
  }

  def createData() = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val providerDAO = ConnectionRepository.getProviderService
    createProviders(providerDAO)
    createServices(serviceDAO, providerDAO)
    val tService = serviceDAO.get("tstrq_service")
    createStreams(tService)
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
    tstrqService.namespace = "test"
    tstrqService.name = "tstrq_service"
    tstrqService.metadataProvider = cassProv
    tstrqService.metadataNamespace = "test_keyspace"
    tstrqService.dataProvider = aeroProv
    tstrqService.dataNamespace = "test"
    tstrqService.lockProvider = zkProv
    tstrqService.lockNamespace = "zk_test"
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
    zkProv.hosts = Array("127.0.0.1:2181")
    providerDAO.save(zkProv)
  }

  def createStreams(tService: Service) = {
    val sjStreamDAO = ConnectionRepository.getStreamService
    val s1 = new SjStream
    s1.name = "s1"
    s1.description = "s1 stream"
    s1.partitions = 7
    s1.service = tService
    s1.tags = "TAG"
    s1.generator = Array("global", "service-zk://zk_service", "2")
    sjStreamDAO.save(s1)

    val s2 = new SjStream
    s2.name = "s2"
    s2.description = "s2 stream"
    s2.partitions = 10
    s2.service = tService
    s2.tags = "TAG"
    s2.generator = Array("per-stream", "service-zk://zk_service", "3")
    sjStreamDAO.save(s2)

    val s3 = new SjStream
    s3.name = "s3"
    s3.description = "s3 stream"
    s3.partitions = 10
    s3.service = tService
    s3.tags = "TAG"
    s3.generator = Array("global", "service-zk://zk_service", "3")
    sjStreamDAO.save(s3)
  }

}
