package com.bwsw.common.tstream

import com.bwsw.common.client.{TcpClient, TcpClientOptions}
import com.bwsw.sj.common.utils.TransactionGeneratorLiterals
import com.bwsw.tstreams.generator.ITransactionGenerator
import org.slf4j.LoggerFactory

/**
 * Provides an entity for generating new transaction ID through tcp client
 * @param zkServers Set of host + port of zookeeper
 * @param prefix Zookeeper path to generator (master of tcp servers)
 * @param retryInterval Delay time between reconnecting attempts to connect to generator
 * @param retryCount Count of attempt to reconnect to generator
 */

class NetworkTransactionGenerator(zkServers: Array[String],
                                  prefix: String,
                                  retryInterval: Int,
                                  retryCount: Int) extends ITransactionGenerator {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val scale = TransactionGeneratorLiterals.scale
  private val options = new TcpClientOptions()
    .setZkServers(zkServers)
    .setPrefix(prefix)
    .setRetryPeriod(retryInterval)
    .setRetryCount(retryCount)
  private val client = new TcpClient(options)

  /**
   * @return ID
   */
  override def getTransaction(): Long = {
    logger.debug("Create a new transaction ID using a tcp client")
    client.get()
  }

  /**
   * @return ID based on timestamp
   */
  override def getTransaction(timestamp: Long): Long = {
    logger.debug("Create a new transaction ID based on a timestamp")
    timestamp * scale
  }
}
