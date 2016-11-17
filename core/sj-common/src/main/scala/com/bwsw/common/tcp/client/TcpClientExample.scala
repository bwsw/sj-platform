package com.bwsw.common.tcp.client

import org.apache.log4j.Logger

/**
 * Main object for running TcpClient
 *
 * @author Kseniya Tomskikh
 */
object TcpClientExample extends App {
  private val logger = Logger.getLogger(getClass)
  val zkServers = Array("176.120.25.19:2181")
  val prefix = "/zk_test/global"

  val options = new TcpClientOptions()
    .setZkServers(zkServers)
    .setPrefix(prefix)

  val client = new TcpClient(options)
  //val consoleReader = new BufferedReader(new InputStreamReader(System.in))
  var i = 0
  val t0 = System.currentTimeMillis()
  while (i <= 1000000) {
    //while (consoleReader.readLine() != null) {
    logger.debug("send request")
    client.get()
    //println(client.get())
    i += 1
  }
  val t1 = System.currentTimeMillis()
  println("Elapsed time: " + (t1 - t0) + "ms")
  client.close()
}