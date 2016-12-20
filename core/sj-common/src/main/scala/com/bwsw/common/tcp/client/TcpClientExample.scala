package com.bwsw.common.tcp.client

import scala.collection.mutable.ArrayBuffer

/**
 * Main object for running TcpClient
 *
 * @author Kseniya Tomskikh
 */
class TcpClientExample {
  val zkServers = Array("176.120.25.19:2181")
  val prefix = "/zk_test/global"

  val options = new TcpClientOptions()
    .setZkServers(zkServers)
    .setPrefix(prefix)

  def main(args: Array[String]): Unit = {
    val client = new TcpClient(options)
    //val consoleReader = new BufferedReader(new InputStreamReader(System.in))
    var i = 0
    val t0 = System.currentTimeMillis()
    while (i <= 1000000) {
      //while (consoleReader.readLine() != null) {
      //logger.debug("send request")
      client.get()
      //println(client.get())
      i += 1
    }
    val t1 = System.currentTimeMillis()
    println("Elapsed time: " + (t1 - t0) + "ms")
    println("OPS: " + 1000000/((t1 - t0)/1000))
    client.close()
  }
}

object MultipleClients {
  private val clients = 100
  private val clientThreads = ArrayBuffer[Thread]()

  def main(args: Array[String]): Unit = {
    (1 to clients).foreach(x => {
      val thread = new Thread(new Runnable {
        override def run(): Unit = {
          new TcpClientExample().main(Array())
        }
      })
      clientThreads.+=(thread)
    })
    clientThreads.foreach(_.start())
    clientThreads.foreach(_.join())
  }
}