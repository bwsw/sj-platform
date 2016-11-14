package com.bwsw.sj.transaction.generator

import java.io.IOException

import com.bwsw.sj.transaction.generator.server.TcpServer
import org.apache.log4j.Logger

/**
 * TCP-Server for transaction generating
 *
 *
 * @author Kseniya Tomskikh
 */
object Server extends App {
  val logger = Logger.getLogger(getClass)
  val zkServers = System.getenv("ZK_SERVERS")
  val host = System.getenv("HOST")
  val port = System.getenv("PORT0").toInt
  val prefix = System.getenv("PREFIX")
  val address = host  + ":" + port

  try {
    val server = new TcpServer(zkServers, prefix, address)

    server.launch()
  } catch {
    case ex: IOException => logger.debug(s"Error: ${ex.getMessage}")
  }
}


