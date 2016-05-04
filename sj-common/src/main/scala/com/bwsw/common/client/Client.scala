package com.bwsw.common.client

import java.io._

import org.apache.log4j.Logger

/**
  * Main object for running TcpClient
  * Created: 18/04/2016
  *
  * @author Kseniya Tomskikh
  */
object Client {

  private val logger = Logger.getLogger(getClass)

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
      logger.debug("send request")
      println(client.get())
      i += 1
    }
    client.close()
  }
}


