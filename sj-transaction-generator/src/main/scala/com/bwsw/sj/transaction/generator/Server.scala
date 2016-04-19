package com.bwsw.sj.transaction.generator

import java.io._

import com.bwsw.sj.common.ConfigLoader
import com.bwsw.sj.transaction.generator.server.TcpServer

/**
  * TCP-Server for transaction generating
  * Created: 18/04/2016
  *
  * @author Kseniya Tomskikh
  */
object Server {

  val conf = ConfigLoader.load()

  def main(args: Array[String]) = {
    val port = conf.getInt("generator-server.port")
    try {
      val serverSocket = new TcpServer(port)
      serverSocket.listen()
    } catch {
      case ex: IOException => println(ex.getMessage)
    }

  }

}


