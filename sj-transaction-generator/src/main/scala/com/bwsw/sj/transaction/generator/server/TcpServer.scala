package com.bwsw.sj.transaction.generator.server

import java.io._
import java.net.{SocketException, ServerSocket, Socket}

import com.datastax.driver.core.utils.UUIDs
import com.twitter.common.zookeeper.DistributedLock.LockingException
import com.twitter.common.zookeeper.{DistributedLockImpl, ZooKeeperClient}
import org.apache.zookeeper.{CreateMode, ZooDefs}

/**
  * TCP-Server for transaction generating
  * Created: 18/04/2016
  *
  * @author Kseniya Tomskikh
  */
class TcpServer(prefix: String, zkClient: ZooKeeperClient, host: String, port: Int) {

  var serverSocket: ServerSocket = null

  def listen() = {
    var isMaster = false
    val distributedLock = new DistributedLockImpl(zkClient, s"$prefix/lock")
    while (!isMaster) {
      try {
        distributedLock.lock()
        serverSocket = new ServerSocket(port)
        updateMaster()
        isMaster = true
      } catch {
        case e: LockingException => Thread.sleep(500)
      }
    }
    println(s"Server $host:$port is started")
    val clientSocket = serverSocket.accept()
    var isWorked = true
    while (isWorked) {
      try {
        val request = readSocket(clientSocket)
        if (request != null && request.equals("TXN")) {
          val newUuid = getNewTransaction.toString
          //println(newUuid)
          writeSocket(clientSocket, newUuid)
        }
      } catch {
        case ex: SocketException => isWorked = false
        case ex: IOException => isWorked = false
      }
    }
    println(s"Server $host:$port is stopped")
  }

  private def readSocket(socket: Socket): String = {
    val bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream))
    bufferedReader.readLine()
  }

  private def writeSocket(socket: Socket, message: String) {
    val out: PrintWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream))
    out.println(message)
    out.flush()
  }

  def getNewTransaction = {
    UUIDs.timeBased()
  }

  private def updateMaster() = {
    val node = s"$prefix/master"
    val value = s"$host:$port".getBytes("UTF-8")
    if (zkClient.get.exists(node, null) != null) {
      zkClient.get().setData(node, value, -1)
    } else {
      zkClient.get().create(node, value, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
    }
  }

}
