package com.bwsw.sj.transaction.generator

import java.io._
import com.bwsw.sj.common.ConfigLoader
import com.bwsw.sj.transaction.generator.client.{TcpClientOptions, TcpClient}

object Client {

  val conf = ConfigLoader.load()

  def main(args: Array[String]) = {
    val zkServers = Array("127.0.0.1:2181")
    val prefix = "servers"
    val retryPeriod = 500
    val retryCount = 10

    val options = new TcpClientOptions()
      .setZkServers(zkServers)
      .setPrefix(prefix)
      .setRetryPeriod(retryPeriod)
      .setRetryCount(retryCount)

    val client = new TcpClient(options)
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


