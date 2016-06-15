package com.bwsw.common.client

import java.io._
import java.net._
import java.util

import com.bwsw.sj.common.ConfigConstants
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.twitter.common.quantity.{Time, Amount}
import com.twitter.common.zookeeper.ZooKeeperClient
import org.apache.log4j.Logger

/**
  * Client for transaction generating
  * Created: 18/04/2016
  *
  * @author Kseniya Tomskikh
  */
class TcpClient(options: TcpClientOptions) {
  private val logger = Logger.getLogger(getClass)
  var socket: Socket = null
  private var retryCount = options.retryCount
  private  val configService = ConnectionRepository.getConfigService
  private val zkSessionTimeout = configService.get(ConfigConstants.zkSessionTimeoutTag).value.toInt

  val zooKeeperServers = new util.ArrayList[InetSocketAddress]()
  options.zkServers.map(x => (x.split(":")(0), x.split(":")(1).toInt))
    .foreach(zkServer => zooKeeperServers.add(new InetSocketAddress(zkServer._1, zkServer._2)))
  val zkClient = new ZooKeeperClient(Amount.of(zkSessionTimeout, Time.MILLISECONDS), zooKeeperServers)

  def open() = {
    var isConnected = false
    while (!isConnected && retryCount > 0) {
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
      logger.info("Could not connect to server")
    } else {
      logger.info("Connected to server")
    }
  }

  def close() = {
    socket.close()
  }

  def get() = {
    retryCount = options.retryCount
    var serverIsNotAvailable = true
    var response = "Server is not available"
    while (serverIsNotAvailable && retryCount > 0) {
      try {
        writeSocket("TXN")
        val fromSocket = readSocket()
        if (fromSocket != null) {
          response = fromSocket
          serverIsNotAvailable = false
          retryCount = options.retryCount
        } else {
          reconnect()
        }
      } catch {
        case ex: Exception => logger.info("Reconnect...")
          reconnect()
      }
    }
    response
  }

  private def connect() = {
    try {
      val master = getMasterServer()
      socket = new Socket(master(0), master(1).toInt)
      socket.setSoTimeout(zkSessionTimeout)
      true
    } catch {
      case ex: ConnectException => false
    }
  }

  private def delay() = {
    Thread.sleep(options.retryPeriod)
    retryCount -= 1
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
    val zkNode = new URI(s"/${options.prefix}/master").normalize()
    val master = new String(zkClient.get().getData(zkNode.toString, null, null), "UTF-8")
    logger.debug(s"Master server: $master")
    master.split(":")
  }

}
