package com.bwsw.common.client

import java.io._
import java.net._
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.util

import com.bwsw.sj.common.utils.ConfigSettingsUtils
import com.twitter.common.quantity.{Amount, Time}
import com.twitter.common.zookeeper.ZooKeeperClient
import org.apache.log4j.Logger

/**
 * Simple tcp client for retrieving transaction ID
 *
 * @author Kseniya Tomskikh
 */
class TcpClient(options: TcpClientOptions) {
  private val logger = Logger.getLogger(getClass)
  private val oneByteForServer = 84
  private var client: SocketChannel = null
  private var retryCount = options.retryCount
  private val zkSessionTimeout = ConfigSettingsUtils.getZkSessionTimeout()
  private val inputBuffer = ByteBuffer.allocate(17)
  private var outputStream: OutputStream = new ByteArrayOutputStream()
  private val zkClient = createZooKeeperClient()

  private def createZooKeeperClient() = {
    val zkServers = createZooKeeperServers()
    new ZooKeeperClient(Amount.of(zkSessionTimeout, Time.MILLISECONDS), zkServers)
  }

  private def createZooKeeperServers() = {
    val zooKeeperServers = new util.ArrayList[InetSocketAddress]()
    options.zkServers.map(x => (x.split(":")(0), x.split(":")(1).toInt))
      .foreach(zkServer => zooKeeperServers.add(new InetSocketAddress(zkServer._1, zkServer._2)))

    zooKeeperServers
  }

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
      outputStream = client.socket().getOutputStream
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

  def get(): Long = {
    retryCount = options.retryCount
    var serverIsNotAvailable = true
    var id: Option[Long] = None
    while (serverIsNotAvailable && retryCount > 0) {
      try {
        sendRequest()
        if (isServerAvailable) {
          id = deserializeResponse()
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
    if (id.isDefined) id.get
    else throw new ConnectException("Server is not available")
  }

  private def sendRequest() {
    outputStream.write(oneByteForServer)
  }

  private def isServerAvailable = {
    val responseStatus = readFromServer()

    responseStatus != -1
  }

  private def readFromServer() = {
    inputBuffer.clear()
    client.read(inputBuffer)
  }

  private def deserializeResponse() = {
    val id = new String(inputBuffer.array(), StandardCharsets.UTF_8).toLong

    Some(id)
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