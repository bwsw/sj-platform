package com.bwsw.sj.transaction.generator

import java.io.IOException
import java.net.InetSocketAddress

import com.bwsw.sj.common.utils.ConfigSettingsUtils
import com.bwsw.sj.transaction.generator.server.TcpServer
import com.twitter.common.quantity.{Time, Amount}
import com.twitter.common.zookeeper.ZooKeeperClient
import org.apache.log4j.Logger

/**
 * TCP-Server for transaction generating
 *
 *
 * @author Kseniya Tomskikh
 */
object Server extends App {
  val logger = Logger.getLogger(getClass)
  val retryPeriod = ConfigSettingsUtils.getServerRetryPeriod()
  val zkServers = System.getenv("ZK_SERVERS")
  val host = System.getenv("HOST")
  val port = System.getenv("PORT0").toInt
  val prefix = System.getenv("PREFIX")

  try {
    val zooKeeperServers = new java.util.ArrayList[InetSocketAddress]()
    zkServers.split(";")
      .map(x => (x.split(":")(0), x.split(":")(1).toInt))
      .foreach(zkServer => zooKeeperServers.add(new InetSocketAddress(zkServer._1, zkServer._2)))
    val zkClient = new ZooKeeperClient(Amount.of(retryPeriod, Time.MILLISECONDS), zooKeeperServers)

    val server = new TcpServer(prefix, zkClient, host, port)
    server.listen()
  } catch {
    case ex: IOException => logger.debug(s"Error: ${ex.getMessage}")
  }
}


