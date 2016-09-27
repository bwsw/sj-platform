package com.bwsw.sj.transaction.generator.server

//class TransactionGenerator(socket: Socket, doesServerWork: AtomicBoolean) extends Runnable {
//  private val counter = new AtomicInteger(0)
//  private val currentMillis = new AtomicLong(0)
//  private val inputStream = new DataInputStream(socket.getInputStream)
//  private val outputStream = new DataOutputStream(socket.getOutputStream)
//  private val scale = TransactionGeneratorLiterals.scale
//
//  override def run(): Unit = {
//    try {
//      while (doesServerWork.get()) {
//        if (isClientAvailable) {
//          val id = generateID()
//          send(id)
//        } else {
//          close()
//          return
//        }
//      }
//    } catch {
//      case ex: Exception =>
//        close()
//    }
//  }
//
//  private def isClientAvailable = {
//    val clientRequestStatus = inputStream.read()
//
//    clientRequestStatus != -1
//  }
//
//  private def generateID() = this.synchronized {
//    val now = System.currentTimeMillis()
//    if (now - currentMillis.get > 0) {
//      currentMillis.set(now)
//      counter.set(0)
//    }
//    now * scale + counter.getAndIncrement()
//  }
//
//  private def send(id: Long) {
//    outputStream.writeLong(id)
//  }
//
//  private def close() = {
//    inputStream.close()
//    outputStream.close()
//    socket.close()
//  }
//}

