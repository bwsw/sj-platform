package com.bwsw.sj.common.utils

import java.net.{InetAddress, InetSocketAddress}

import com.bwsw.tstreams.common.{CassandraConnectorConf, CassandraHelper}
import com.bwsw.tstreams.data.cassandra.Factory
import com.bwsw.tstreams.metadata.MetadataStorageFactory
import com.datastax.driver.core.{Cluster, Session}

/**
 * Test util for creating C* entities
 */
class CassandraFactory {
  import scala.collection.JavaConverters._

  private var cluster: Cluster = null
  private var session: Session = null
  private val metadataStorageFactory = new MetadataStorageFactory()
  private val dataStorageFactory = new Factory()
  private var cassandraConnectorConf: CassandraConnectorConf = null

  def open(cassandraHosts: Set[InetSocketAddress]) = {
    cluster = Cluster.builder().addContactPointsWithPorts(cassandraHosts.toList.asJava).build()
    session = cluster.connect()
    cassandraConnectorConf = CassandraConnectorConf.apply(cassandraHosts)
  }

  def getDataStorage(keyspace: String) = {
    dataStorageFactory.getInstance(
      cassandraConnectorConf,
      keyspace = keyspace)
  }

  def getMetadataStorage(keyspace: String) = {
    metadataStorageFactory.getInstance(
      cassandraConnectorConf,
      keyspace = keyspace)
  }

  def createKeyspace(keyspace: String) = {
    CassandraHelper.createKeyspace(session, keyspace)
  }

  def createDataTable(keyspace: String) = {
    CassandraHelper.createDataTable(session, keyspace)
  }

  def createMetadataTables(keyspace: String) = {
    CassandraHelper.createMetadataTables(session, keyspace)
  }

  def dropKeyspace(keyspace: String) = {
    CassandraHelper.dropKeyspace(session, keyspace)
  }

  def close() = {
    metadataStorageFactory.closeFactory()
    session.close()
    cluster.close()
  }
}
