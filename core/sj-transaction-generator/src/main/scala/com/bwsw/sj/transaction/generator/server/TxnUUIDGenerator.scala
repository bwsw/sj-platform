package com.bwsw.sj.transaction.generator.server

import java.io.{OutputStreamWriter, PrintWriter}
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

import com.datastax.driver.core.utils.UUIDs

class TxnUUIDGenerator(socket: Socket, doesServerWork: AtomicBoolean) extends Runnable {

  override def run(): Unit = {
    try {
      while (doesServerWork.get()) {
        if (isClientAvailable) {
          val uuid = generateUUID()
          send(uuid)
        } else {
          close()
          return
        }
      }
    } catch {
      case ex: Exception =>
        close()
    }
  }

  private def isClientAvailable = {
    val inputStream = socket.getInputStream
    val clientRequestStatus = inputStream.read()

    clientRequestStatus != -1
  }

  private def generateUUID() = {
    UUIDs.timeBased().toString
  }

  private def send(message: String) {
    val out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream))
    out.print(message)
    out.flush()
  }

  private def close() = socket.close()
}

