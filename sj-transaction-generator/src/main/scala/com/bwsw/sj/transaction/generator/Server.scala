package com.bwsw.sj.transaction.generator

import java.io._
import java.net.InetSocketAddress
import java.util

import com.bwsw.sj.common.ConfigLoader
import com.bwsw.sj.transaction.generator.server.TcpServer
import com.twitter.common.quantity.{Time, Amount}
import com.twitter.common.zookeeper.ZooKeeperClient

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
    val host = System.getenv("HOST")
    val port = System.getenv("PORT").toInt
    val prefix = System.getenv("PREFIX")

    try {
      val zooKeeperServers = new util.ArrayList[InetSocketAddress]()
      zkServers.split(";")
        .map(x => (x.split(":")(0), x.split(":")(1).toInt))
        .foreach(zkServer => zooKeeperServers.add(new InetSocketAddress(zkServer._1, zkServer._2)))
      val zkClient = new ZooKeeperClient(Amount.of(500, Time.MILLISECONDS), zooKeeperServers)

      val server = new TcpServer(s"/$prefix", zkClient, host, port)
      server.listen()
    } catch {
      case ex: IOException => println(ex.getMessage)
    }

  }

}


