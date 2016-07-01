package com.bwsw.sj.crud.rest.utils

import java.net.InetAddress

import com.bwsw.sj.common.DAL.model._
import com.datastax.driver.core.{Session, Cluster}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
  * Util objects for work with services
  * Created: 01/07/2016
  *
  * @author Kseniya Tomskikh
  */
object ServiceUtil {

  private val logger = LoggerFactory.getLogger(getClass.getName)

  /**
    * Prepare service: create keyspaces/namespaces/indexes/metatables
    *
    * @param service Service object
    */
  def prepareService(service: Service) = {
    service match {
      case esService: ESService => createIndex(esService)

      case cassService: CassandraService =>
        val session = openCassandraSession(cassService.provider)
        createKeyspace(cassService.keyspace, session)
        closeCassandraSession(session)

      case tService: TStreamService => createTStreamService(tService)

      case _ =>
    }
  }

  /**
    * Create elasticsearch index
    *
    * @param esService Elasticsearch service
    */
  private def createIndex(esService: ESService) = {
    val client: TransportClient = TransportClient.builder().build()
    esService.provider.hosts.foreach { host =>
      val parts = host.split(":")
      client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(parts(0)), parts(1).toInt))
    }
    val isIndexExist = client.admin().indices().prepareExists(esService.index).execute().actionGet()
    if (!isIndexExist.isExists) {
      client.admin().indices().prepareCreate(esService.index).execute().actionGet()
      logger.debug(s"Elasicsearch service ${esService.name}. Index ${esService.index} is create.")
    }
  }

  /**
    * Create namespaces and metadata tables for t-stream service
    *
    * @param tStreamService TStream service
    */
  private def createTStreamService(tStreamService: TStreamService) = {
    val session = openCassandraSession(tStreamService.metadataProvider)
    createKeyspace(tStreamService.metadataNamespace, session)
    createMetatables(tStreamService.metadataNamespace, session)

    if (tStreamService.dataProvider.providerType.equals("cassandra")) {
      val dataSession = openCassandraSession(tStreamService.dataProvider)
      createKeyspace(tStreamService.dataNamespace, dataSession)
      createMetatables(tStreamService.dataNamespace, dataSession)
      closeCassandraSession(dataSession)
    }
    closeCassandraSession(session)
  }

  /**
    * Create cassandra keyspace
    *
    * @param keyspace Keyspace name
    * @param session Cassandra session
    */
  private def createKeyspace(keyspace: String, session: Session) = {
    session.execute(s"CREATE KEYSPACE IF NOT EXISTS $keyspace WITH replication = " +
      s" {'class': 'SimpleStrategy', 'replication_factor': '1'} " +
      s" AND durable_writes = true")

    logger.debug("")
  }

  /**
    * Create metatables for t-stream
    *
    * @param keyspace Name of cassandra keyspace
    * @param session Cassandra session
    */
  private def createMetatables(keyspace: String, session: Session) = {
    session.execute(s"CREATE TABLE IF NOT EXISTS $keyspace.data_queue ( " +
      s"stream text, " +
      s"partition int, " +
      s"transaction timeuuid, " +
      s"seq int, " +
      s"data blob, " +
      s"PRIMARY KEY ((stream, partition), transaction, seq))")

    session.execute(s"CREATE TABLE IF NOT EXISTS $keyspace.stream_commit_last (" +
      s"stream text, " +
      s"partition int, " +
      s"transaction timeuuid, " +
      s"PRIMARY KEY (stream, partition))")

    session.execute(s"CREATE TABLE IF NOT EXISTS $keyspace.consumers (" +
      s"name text, " +
      s"stream text, " +
      s"partition int, " +
      s"last_transaction timeuuid, " +
      s"PRIMARY KEY (name, stream, partition))")

    session.execute(s"CREATE TABLE IF NOT EXISTS $keyspace.streams (" +
      s"stream_name text PRIMARY KEY, " +
      s"partitions int," +
      s"ttl int, " +
      s"description text)")

    session.execute(s"CREATE TABLE IF NOT EXISTS $keyspace.commit_log (" +
      s"stream text, " +
      s"partition int, " +
      s"transaction timeuuid, " +
      s"cnt int, " +
      s"PRIMARY KEY (stream, partition, transaction))")

    session.execute(s"CREATE TABLE IF NOT EXISTS $keyspace.generators (" +
      s"name text, " +
      s"time timeuuid, " +
      s"PRIMARY KEY (name))")

    logger.debug("Metatables for t-stream is created.")
  }

  /**
    * Open new session to cassandra server
    *
    * @param provider Cassandra provider for connection to server
    * @return New cassandra session
    */
  private def openCassandraSession(provider: Provider) : Session = {
    val cassandraHosts = provider.hosts.map { host =>
      val parts = host.split(":")
      InetAddress.getByName(parts(0))
    }.toList.asJava
    val cluster = Cluster.builder().addContactPoints(cassandraHosts).build()
    cluster.connect()
  }

  /**
    * Close cassandra session
    *
    * @param session Cassandra session
    */
  private def closeCassandraSession(session: Session) = {
    session.close()
  }

}
