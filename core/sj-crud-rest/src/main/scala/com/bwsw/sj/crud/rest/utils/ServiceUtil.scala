package com.bwsw.sj.crud.rest.utils

import java.net.{InetAddress, InetSocketAddress}

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.utils.CassandraFactory
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.slf4j.LoggerFactory

object ServiceUtil {

  private val logger = LoggerFactory.getLogger(getClass.getName)

  /**
   * Prepare service: create keyspace/namespace/index/metadata tables/data tables
   *
   * @param service Service object
   */
  def prepareService(service: Service) = {
    logger.info(s"Prepare service ${service.name}.")
    service match {
      case esService: ESService => createIndex(esService)
      case cassService: CassandraService => createKeyspace(cassService)
      case tService: TStreamService => createTStreamService(tService)
      case _ =>
    }
  }

  private def createIndex(esService: ESService) = {
    logger.info(s"Check and create elasticsearch index ${esService.index}.")
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

  private def createKeyspace(cassService: CassandraService) = {
    val cassandraFactory = new CassandraFactory
    cassandraFactory.open(getCassandraHosts(cassService.provider))
    cassandraFactory.createKeyspace(cassService.keyspace)
    cassandraFactory.close()
  }

  /**
   * Create namespace, metadata and data tables (if data provider has cassandra type) for t-stream service
   */
  private def createTStreamService(tStreamService: TStreamService) = {
    logger.info(s"Create cassandra keyspace ${tStreamService.metadataNamespace}.")
    val cassandraFactory = new CassandraFactory
    cassandraFactory.open(getCassandraHosts(tStreamService.metadataProvider))
    cassandraFactory.createKeyspace(tStreamService.metadataNamespace)
    cassandraFactory.createMetadataTables(tStreamService.metadataNamespace)

    if (tStreamService.dataProvider.providerType.equals("cassandra")) {
      val cassandraFactory = new CassandraFactory
      cassandraFactory.open(getCassandraHosts(tStreamService.metadataProvider))
      cassandraFactory.createKeyspace(tStreamService.dataNamespace)
      cassandraFactory.createDataTable(tStreamService.dataNamespace)
      cassandraFactory.close()
    } //todo possible there is a necessary of creation namespace of other provider (aerospike)
    cassandraFactory.close()
  }

  def deleteService(service: Service) = {
    service match {
      case esService: ESService => deleteIndex(esService)
      case cassService: CassandraService => deleteKeyspace(cassService)
      case _ =>
    }
  }

  private def deleteIndex(esService: ESService) = {
    logger.info(s"Delete elasticsearch index ${esService.index}.")
    val client: TransportClient = TransportClient.builder().build()
    esService.provider.hosts.foreach { host =>
      val parts = host.split(":")
      client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(parts(0)), parts(1).toInt))
    }
    client.admin().indices().prepareDelete(esService.index).execute().actionGet()
    logger.debug(s"Elasticsearch service ${esService.name}. Index ${esService.index} is create.")
  }

  private def deleteKeyspace(cassService: CassandraService) = {
    logger.info(s"Delete cassandra keyspace ${cassService.keyspace}.")
    val cassandraFactory = new CassandraFactory
    cassandraFactory.open(getCassandraHosts(cassService.provider))
    cassandraFactory.dropKeyspace(cassService.keyspace)
    cassandraFactory.close()
  }

  private def getCassandraHosts(provider: Provider) = {
    logger.debug(s"Open cassandra connection. Provider: ${provider.name}.")
    val cassandraHosts = provider.hosts.map { host =>
      val parts = host.split(":")
      new InetSocketAddress(parts(0), parts(1).toInt)
    }.toSet

    cassandraHosts
  }
}
