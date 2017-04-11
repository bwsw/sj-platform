//package com.bwsw.sj.common.utils
//
//import java.net.InetSocketAddress
//
//import com.bwsw.tstreams.common.{CassandraConnectorConf, CassandraHelper}
//import com.bwsw.tstreams.data.cassandra.Factory
//import com.bwsw.tstreams.metadata.MetadataStorageFactory
//import com.datastax.driver.core.{Cluster, Session}
//
//class CassandraFactory {
//
//  import scala.collection.JavaConverters._
//
//  private var cluster: Cluster = null
//  private var session: Session = null
//  private var cassandraConnectorConf: CassandraConnectorConf = null
//  private val metadataStorageFactory = new MetadataStorageFactory()
//  private val dataStorageFactory = new Factory()
//
//  def open(hosts: Set[(String, Int)]) = {
//    val cassandraHosts = hosts.map(s => new InetSocketAddress(s._1, s._2))
//    cluster = Cluster.builder().addContactPointsWithPorts(cassandraHosts.toList.asJava).build()
//    session = cluster.connect()
//    cassandraConnectorConf = CassandraConnectorConf.apply(cassandraHosts)
//  }
//
//  def getDataStorage(keyspace: String) = {
//    dataStorageFactory.getInstance(
//      cassandraConnectorConf,
//      keyspace = keyspace)
//  }
//
//  def getMetadataStorage(keyspace: String) = {
//    metadataStorageFactory.getInstance(
//      cassandraConnectorConf,
//      keyspace = keyspace)
//  }
//
//  def createKeyspace(keyspace: String) = {
//    CassandraHelper.createKeyspace(session, keyspace)
//  }
//
//  def createDataTable(keyspace: String) = {
//    CassandraHelper.createDataTable(session, keyspace)
//  }
//
//  def createMetadataTables(keyspace: String) = {
//    CassandraHelper.createMetadataTables(session, keyspace)
//  }
//
//  def dropKeyspace(keyspace: String) = {
//    CassandraHelper.dropKeyspace(session, keyspace)
//  }
//
//  def close() = {
//    metadataStorageFactory.closeFactory()
//    session.close()
//    cluster.close()
//  }
//}//todo after integration with t-streams