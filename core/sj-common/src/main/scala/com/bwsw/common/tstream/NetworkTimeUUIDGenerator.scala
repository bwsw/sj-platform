package com.bwsw.common.tstream

import java.util.UUID

import com.bwsw.common.client.{TcpClient, TcpClientOptions}
import com.bwsw.tstreams.generator.IUUIDGenerator
import com.datastax.driver.core.utils.UUIDs
import org.slf4j.LoggerFactory

/**
 * Provides an entity for generating new txn time UUID through tcp client
 * @param zkServers Set of host + port of zookeeper
 * @param prefix Zookeeper path to generator (master of tcp servers)
 * @param retryInterval Delay time between reconnecting attempts to connect to generator
 * @param retryCount Count of attempt to reconnect to generator
 */

class NetworkTimeUUIDGenerator(zkServers: Array[String],
                              prefix: String,
                              retryInterval: Int,
                              retryCount: Int) extends IUUIDGenerator {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val options = new TcpClientOptions()
    .setZkServers(zkServers)
    .setPrefix(prefix)
    .setRetryPeriod(retryInterval)
    .setRetryCount(retryCount)

  private val client = new TcpClient(options)
  client.open()

  /**
   * @return Transaction UUID
   */
  override def getTimeUUID(): UUID = {
    logger.debug("Create a new transaction UUID using a tcp client")
    UUID.fromString(client.get())
  }

  /**
   * @return UUID based on timestamp
   */
  override def getTimeUUID(timestamp: Long): UUID = {
    logger.debug("Create a new transaction UUID based on a timestamp")
    UUIDs.startOf(timestamp)
  }

}
