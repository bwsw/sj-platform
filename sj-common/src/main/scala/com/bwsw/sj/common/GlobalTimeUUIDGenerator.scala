package com.bwsw.sj.common

import java.util.UUID

import com.bwsw.common.client.{TcpClient, TcpClientOptions}
import com.bwsw.tstreams.generator.IUUIDGenerator

/**
 * Provides a generator of txn UUID for TStreams through Tcp client
 * @param zkServers Set of host + port of zookeeper
 * @param prefix Zookeeper path to generator (master of Tcp servers)
 * @param retryPeriod Delay time for reconnect to generator
 * @param retryCount Count of attempt to connect to generator
 */
class GlobalTimeUUIDGenerator(zkServers: Array[String],
                              prefix: String,
                              retryPeriod: Long,
                              retryCount: Int) extends IUUIDGenerator {

  private val options = new TcpClientOptions()
    .setZkServers(zkServers)
    .setPrefix(prefix)
    .setRetryPeriod(retryPeriod)
    .setRetryCount(retryCount)

  private val client = new TcpClient(options)
  client.open()

  override def getTimeUUID(): UUID = UUID.fromString(client.get())

  override def getTimeUUID(timestamp: Long): UUID = ???

}
