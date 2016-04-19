package com.bwsw.sj.transaction.generator

import java.io._

import com.bwsw.sj.common.ConfigLoader
import com.bwsw.sj.transaction.generator.client.TcpClient

/**
  * Client for transaction generating
  * Created: 18/04/2016
  *
  * @author Kseniya Tomskikh
  */
object Client {

  val conf = ConfigLoader.load()

  def main(args: Array[String]) = {
    val host = conf.getString("generator-server.host")
    val port = conf.getInt("generator-server.port")

    val client = new TcpClient(host, port)
    client.open()

    val consoleReader = new BufferedReader(new InputStreamReader(System.in))
    var i = 0
    while(consoleReader.readLine() != null) {
      println(client.get())
      i += 1
    }
    client.close()
  }
}


