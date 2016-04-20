package com.bwsw.sj.transaction.generator.client

import java.io.{BufferedReader, InputStreamReader, OutputStreamWriter, PrintWriter}
import java.net.{InetSocketAddress, Socket}
import java.util

import com.twitter.common.quantity.{Time, Amount}
import com.twitter.common.zookeeper.{DistributedLockImpl, ZooKeeperClient}

/**
  * Client for transaction generating
  * Created: 18/04/2016
  *
  * @author Kseniya Tomskikh
  */
class TcpClient(host: String, port: Int) {
  var socket: Socket = null

  val zooKeeperServers = new util.ArrayList[InetSocketAddress]()
  zooKeeperServers.add(new InetSocketAddress(2181))
  val zkClient = new ZooKeeperClient(Amount.of(0, Time.DAYS), zooKeeperServers)

  val retryPeriod = 500
  val retryCount = 10

  def open() = {
    var isConnected = false
    while (!isConnected) {
      val master = zkClient.get().getChildren("/zk_servers/lock", null)
      if (master.size() > 0) {
        socket = new Socket(host, port)
        isConnected = true
      }
      Thread.sleep(500)
    }
  }

  def close() = {
    socket.close()
  }

  def get() = {
    writeSocket("GET TRANSACTION")
    readSocket()
  }

  private def readSocket(): String = {
    val bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream))
    bufferedReader.readLine()
  }

  private def writeSocket(message: String) {
    val out: PrintWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream))
    out.println(message)
    out.flush()
  }

  private def getMaster() = {
    val master = zkClient.get().getChildren("/zk_servers/lock", null)
    master
  }

}
