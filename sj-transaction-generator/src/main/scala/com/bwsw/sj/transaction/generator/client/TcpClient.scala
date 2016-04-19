package com.bwsw.sj.transaction.generator.client

import java.io.{BufferedReader, InputStreamReader, OutputStreamWriter, PrintWriter}
import java.net.Socket

/**
  * Created: 4/18/16
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

  def readSocket(): String = {
    val bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream))
    bufferedReader.readLine()
  }

  def writeSocket(string: String) {
    val out: PrintWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream))
    out.println(string)
    out.flush()
  }

}
