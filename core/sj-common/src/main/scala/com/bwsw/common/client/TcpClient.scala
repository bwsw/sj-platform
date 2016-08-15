package com.bwsw.common.client

import java.io._
import java.net._
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
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
  var client: SocketChannel = null
  private var retryCount = options.retryCount
  private val configService = ConnectionRepository.getConfigService
  private val zkSessionTimeout = configService.get(ConfigConstants.zkSessionTimeoutTag).value.toInt

  val zooKeeperServers = new util.ArrayList[InetSocketAddress]()
  options.zkServers.map(x => (x.split(":")(0), x.split(":")(1).toInt))
    .foreach(zkServer => zooKeeperServers.add(new InetSocketAddress(zkServer._1, zkServer._2)))
  val zkClient = new ZooKeeperClient(Amount.of(zkSessionTimeout, Time.MILLISECONDS), zooKeeperServers)
  private val inputBuffer = ByteBuffer.allocate(36)

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
      throw new Exception("Could not connect to server")
    } else {
      logger.info("Connected to server")
    }
  }

  private def connect() = {
    try {
      val master = getMasterServer()
      client = SocketChannel.open(new InetSocketAddress(master(0), master(1).toInt))
      client.socket.setSoTimeout(zkSessionTimeout)
      true
    } catch {
      case ex: IOException => false
    }
  }

  private def getMasterServer() = {
    val zkNode = new URI(s"/${options.prefix}/master").normalize()
    val master = new String(zkClient.get().getData(zkNode.toString, null, null), "UTF-8")
    logger.debug(s"Master server: $master")
    master.split(":")
  }

  def get() = {
    retryCount = options.retryCount
    var serverIsNotAvailable = true
    var uuid: Option[String] = None
    while (serverIsNotAvailable && retryCount > 0) {
      try {
        sendRequest()
        if (isServerAvailable) {
          uuid = deserializeResponse()
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
    if (uuid.isDefined) uuid.get
    else throw new ConnectException("Server is not available")
  }

  private def sendRequest() {
    val bs = "x".getBytes(StandardCharsets.UTF_8)
    val buffer = ByteBuffer.wrap(bs)
    client.write(buffer)
    buffer.clear()
  }

  private def isServerAvailable = {
    val responseStatus = readFromServer()

    responseStatus != -1
  }

  private def readFromServer() = {
    client.read(inputBuffer)
  }

  private def deserializeResponse() = {
    val uuid = new String(inputBuffer.array(), StandardCharsets.UTF_8)
    inputBuffer.clear()

    Some(uuid)
  }

  private def reconnect() = {
    close()
    delay()
    connect()
  }

  def close() = {
    client.close()
  }

  private def delay() = {
    Thread.sleep(options.retryPeriod)
    retryCount -= 1
  }
}
