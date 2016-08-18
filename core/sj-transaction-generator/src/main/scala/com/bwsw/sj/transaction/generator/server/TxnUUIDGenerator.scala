package com.bwsw.sj.transaction.generator.server

import java.io.{DataInputStream, PrintStream}
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

import com.datastax.driver.core.utils.UUIDs

class TxnUUIDGenerator(socket: Socket, doesServerWork: AtomicBoolean) extends Runnable {
  val inputStream = new DataInputStream(socket.getInputStream)
  val outputStream = new PrintStream(socket.getOutputStream)

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
    val clientRequestStatus = inputStream.read()

    clientRequestStatus != -1
  }

  private def generateUUID() = {
    UUIDs.timeBased().toString
  }

  private def send(message: String) {
    outputStream.print(message)
  }

  private def close() = {
    inputStream.close()
    outputStream.close()
    socket.close()
  }
}

