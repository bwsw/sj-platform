package com.bwsw.sj.transaction.generator.server

import java.io.{BufferedReader, InputStreamReader, OutputStreamWriter, PrintWriter}
import java.net.{InetSocketAddress, ServerSocket, Socket}
import java.util

import com.datastax.driver.core.utils.UUIDs
import com.twitter.common.quantity.{Time, Amount}
import com.twitter.common.zookeeper.DistributedLock.LockingException
import com.twitter.common.zookeeper.{DistributedLock, ZooKeeperClient}

/**
  * TCP-Server for transaction generating
  * Created: 18/04/2016
  *
  * @author Kseniya Tomskikh
  */
class TcpServer(distributedLock: DistributedLock, host: String, port: Int) {

  var serverSocket: ServerSocket = null

  def listen() = {
    var isMaster = false
    while (!isMaster) {
      try {
        distributedLock.lock()
        serverSocket = new ServerSocket(port)
        isMaster = true
      } catch {
        case e: LockingException => Thread.sleep(500)
      }
    }
    val clientSocket = serverSocket.accept()
    while (true) {
      val request = readSocket(clientSocket)
      if (request != null && request.equals("GET TRANSACTION")) {
        val newUuid = getNewTransaction.toString
        //println(newUuid)
        writeSocket(clientSocket, newUuid)
      }
    }
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

}
