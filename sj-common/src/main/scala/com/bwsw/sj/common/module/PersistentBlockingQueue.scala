package com.bwsw.sj.common.module


import java.nio.file.{Files, Path}
import java.util.concurrent.locks._

import net.openhft.chronicle.queue.ChronicleQueueBuilder

/**
 * Provides a blocking queue to keep Transactions from sub. consumers and than pass them into module run function
 * @param path Temporary directory path for queue
 */
class PersistentBlockingQueue(path: String) {

  private val tempDirectory: Path = Files.createTempDirectory(path)
  tempDirectory.toFile.deleteOnExit()
  val chronicleQueue = ChronicleQueueBuilder.single(tempDirectory.toString).build()
  private val writer = chronicleQueue.createAppender()
  private val reader = chronicleQueue.createTailer()
  private val mutex = new ReentrantLock(true)
  private val cond = mutex.newCondition()

  def put(k: String) = {
    mutex.lock()
    writer.writeText(k)
    cond.signal()
    mutex.unlock()
  }

  def get() = {
    mutex.lock()
    var data = reader.readText()
    if (data == null) {
      cond.await()
      data = reader.readText()
    }
    mutex.unlock()
    data
  }
}
