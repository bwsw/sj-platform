package com.bwsw.sj.transaction.generator

import java.io._
import java.net.InetSocketAddress
import java.util

import com.bwsw.sj.common.ConfigLoader
import com.bwsw.sj.transaction.generator.server.TcpServer
import com.twitter.common.quantity.{Time, Amount}
import com.twitter.common.zookeeper.{DistributedLockImpl, ZooKeeperClient}

/**
  * TCP-Server for transaction generating
  * Created: 18/04/2016
  *
  * @author Kseniya Tomskikh
  */
object Server {

  val conf = ConfigLoader.load()

  def main(args: Array[String]) = {
    val zkServers = System.getenv("ZK_SERVERS")
    val zkHost = System.getenv("ZK_HOST")
    val zkPort = System.getenv("ZK_PORT").toInt
    if (zkServers == null || zkServers.isEmpty) {
      println("ERROR: Environment 'ZK_SERVERS' has not been found")
    } else {
      val zooKeeperServers = new util.ArrayList[InetSocketAddress]()
      zooKeeperServers.add(new InetSocketAddress(zkHost, zkPort))
      val zkClient = new ZooKeeperClient(Amount.of(1, Time.MINUTES), zooKeeperServers)
      val distributedLock = new DistributedLockImpl(zkClient, s"/zk_servers/lock")

      val servers = zkServers.split(";")
        .map(x => (x.split(":")(0), x.split(":")(1).toInt))
        .map(s => new TcpServer(distributedLock, s._1, s._2))
      try {
        servers.foreach(server => server.listen())
      } catch {
        case ex: IOException => println(ex.getMessage)
      }
    }

  }

}


