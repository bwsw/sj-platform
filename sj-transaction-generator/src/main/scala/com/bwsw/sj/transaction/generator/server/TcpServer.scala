package com.bwsw.sj.transaction.generator.server

import java.io.{BufferedReader, InputStreamReader, OutputStreamWriter, PrintWriter}
import java.net.{ServerSocket, Socket}

import com.datastax.driver.core.utils.UUIDs

/**
  * TCP-Server for transaction generating
  * Created: 18/04/2016
  *
  * @author Kseniya Tomskikh
  */
class TcpServer(port: Int) {

  val serverSocket = new ServerSocket(port)

  def listen() = {
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
