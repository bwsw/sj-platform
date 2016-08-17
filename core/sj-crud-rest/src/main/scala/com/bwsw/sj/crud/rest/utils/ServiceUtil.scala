package com.bwsw.sj.crud.rest.utils

import java.net.{InetAddress, InetSocketAddress}

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.utils.CassandraFactory
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.slf4j.LoggerFactory

/**
 * Util objects for work with services
 * Created: 01/07/2016
 *
 * @author Kseniya Tomskikh
 */
object ServiceUtil {

  private val logger = LoggerFactory.getLogger(getClass.getName)
  private val cassandraFactory = new CassandraFactory

  /**
   * Prepare service: create keyspaces/namespaces/indexes/metatables
   *
   * @param service Service object
   */
  def prepareService(service: Service) = {
    logger.info(s"Prepare service ${service.name}.")
    service match {
      case esService: ESService => createIndex(esService)

      case cassService: CassandraService =>
        openCassandraSession(cassService.provider)
        cassandraFactory.createKeyspace(cassService.keyspace)
        cassandraFactory.close()

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

  /**
   * Create namespaces and metadata tables for t-stream service
   *
   * @param tStreamService TStream service
   */
  private def createTStreamService(tStreamService: TStreamService) = {
    openCassandraSession(tStreamService.metadataProvider)
    cassandraFactory.createKeyspace(tStreamService.metadataNamespace)
    cassandraFactory.createMetadataTables(tStreamService.metadataNamespace)

    if (tStreamService.dataProvider.providerType.equals("cassandra")) {
      openCassandraSession(tStreamService.dataProvider)
      cassandraFactory.createKeyspace(tStreamService.dataNamespace)
      cassandraFactory.createDataTable(tStreamService.dataNamespace)
      cassandraFactory.close()
    }
    cassandraFactory.close()
  }

  /**
   * Open new session to cassandra server
   *
   * @param provider Cassandra provider for connection to server
   * @return New cassandra session
   */
  private def openCassandraSession(provider: Provider) = {
    logger.debug(s"Open cassandra connection. Provider: ${provider.name}.")
    val cassandraHosts = provider.hosts.map { host =>
      val parts = host.split(":")
      new InetSocketAddress(parts(0), parts(1).toInt)
    }.toSet
    cassandraFactory.open(cassandraHosts)
  }
}
