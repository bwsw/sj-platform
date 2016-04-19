package com.bwsw.sj.transaction.generator.client

import java.io.{BufferedReader, InputStreamReader, OutputStreamWriter, PrintWriter}
import java.net.Socket

/**
  * Client for transaction generating
  * Created: 18/04/2016
  *
  * @author Kseniya Tomskikh
  */
class TcpClient(host: String, port: Int) {
  var socket: Socket = null

  def open() = {
    socket = new Socket(host, port)
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

}
