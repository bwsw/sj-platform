package com.bwsw.sj.transaction.generator

import com.bwsw.sj.transaction.generator.server.TcpServer

/**
 * TCP-Server for transaction generating
 *
 *
 * @author Kseniya Tomskikh
 */
object Server extends App {
  private val host = System.getenv("HOST")
  private val port = System.getenv("PORT0").toInt
  private val server = new TcpServer(host, port)

  server.listen()
}


