package com.bwsw.sj.transaction.generator.server

import java.io._
import java.net.{ServerSocket, URI}
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

import com.bwsw.sj.common.ConfigConstants
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.twitter.common.zookeeper.DistributedLock.LockingException
import com.twitter.common.zookeeper.{DistributedLockImpl, ZooKeeperClient}
import org.apache.log4j.Logger
import org.apache.zookeeper.{CreateMode, ZooDefs}

/**
 * TCP-Server for transaction generating
 * Created: 18/04/2016
 *
 * @author Kseniya Tomskikh
 */
class TcpServer(prefix: String, zkClient: ZooKeeperClient, host: String, port: Int) {
  private val logger = Logger.getLogger(getClass)
  private val retryPeriod = getRetryPeriod()
  private val executorService = createExecutorService()

  var serverSocket: ServerSocket = null

  private def getRetryPeriod() = {
    val configService = ConnectionRepository.getConfigService

    configService.get(ConfigConstants.tgServerRetryPeriodTag).value.toInt
  }

  private def createExecutorService() = {
    val countOfThreads = 60
    val threadFactory = createThreadFactory()

    Executors.newFixedThreadPool(countOfThreads, threadFactory)
  }

  private def createThreadFactory() = {
    new ThreadFactoryBuilder()
      .setNameFormat("TCPServer-%d")
      .build()
  }

  def listen() = {
    var isMaster = false
    val zkLockNode = new URI(s"/$prefix/lock").normalize()
    val distributedLock = new DistributedLockImpl(zkClient, zkLockNode.toString)
    while (!isMaster) {
      try {
        distributedLock.lock()
        serverSocket = new ServerSocket(port)
        updateMaster()
        isMaster = true
      } catch {
        case e: LockingException => Thread.sleep(retryPeriod)
      }
    }
    logger.info(s"Server $host:$port is started")
    val doesServerWork = new AtomicBoolean(true)
    try {
      while (doesServerWork.get()) {
        val newClient = serverSocket.accept()
        val txnUUIDGenerator = new TxnUUIDGenerator(newClient, doesServerWork)
        executorService.execute(txnUUIDGenerator)
      }
    } catch {
      case exception: IOException => {
        doesServerWork.set(false)
        logger.error(s"Server $host:$port is stopped", exception)
      }
    }
  }

  private def updateMaster() = {
    val node = new URI(s"/$prefix/master").normalize().toString
    val value = s"$host:$port".getBytes("UTF-8")
    if (zkClient.get.exists(node, null) != null) {
      zkClient.get().setData(node, value, -1)
    } else {
      zkClient.get().create(node, value, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
    }
    logger.debug("Master server updated")
  }
}