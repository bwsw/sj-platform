package com.bwsw.sj.transaction.generator.client

import java.io._
import java.net._
import java.util

import com.twitter.common.quantity.{Time, Amount}
import com.twitter.common.zookeeper.ZooKeeperClient

/**
  * Client for transaction generating
  * Created: 18/04/2016
  *
  * @author Kseniya Tomskikh
  */
class TcpClient(options: TcpClientOptions) {
  var socket: Socket = null

  val zooKeeperServers = new util.ArrayList[InetSocketAddress]()
  options.zkServers.map(x => (x.split(":")(0), x.split(":")(1).toInt))
    .foreach(zkServer => zooKeeperServers.add(new InetSocketAddress(zkServer._1, zkServer._2)))
  val zkClient = new ZooKeeperClient(Amount.of(500, Time.MILLISECONDS), zooKeeperServers)

  def open() = {
    var isConnected = false
    while (!isConnected && options.retryCount > 0) {
      try {
        isConnected = connect()
        if (!isConnected) {
          delay()
        }
      } catch {
        case ex: Exception => delay()
      }
    }
    if (!isConnected) {
      println("Could not connect to server")
    } else {
      println("Connected to server")
    }
  }

  def close() = {
    socket.close()
  }

  def get() = {
    var serverIsNotAvailable = true
    var response = "Server is not available"
    while (serverIsNotAvailable && options.retryCount > 0) {
      try {
        writeSocket("TXN")
        val fromSocket = readSocket()
        if (fromSocket != null) {
          response = fromSocket
          serverIsNotAvailable = false
        } else {
          reconnect()
        }
      } catch {
        case ex: Exception => reconnect()
      }
    }

    response
  }

  private def connect() = {
    try {
      val master = getMasterServer()
      socket = new Socket(master(0), master(1).toInt)
      socket.setSoTimeout(500)
      true
    } catch {
      case ex: ConnectException => false
    }
  }

  private def delay() = {
    Thread.sleep(options.retryPeriod)
    options.retryCount -= 1
  }

  private def reconnect() = {
    close()
    delay()
    connect()
  }

  private def readSocket(): String = {
    val stream = socket.getInputStream
    val bufferedReader = new BufferedReader(new InputStreamReader(stream))
    bufferedReader.readLine()
  }

  private def writeSocket(message: String) {
    val out: PrintWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream))
    out.println(message)
    out.flush()
  }

  private def getMasterServer() = {
    val master = new String(zkClient.get().getData(s"/${options.prefix}/master", null, null), "UTF-8")
    println(s"Master server: $master")
    master.split(":")
  }

}
