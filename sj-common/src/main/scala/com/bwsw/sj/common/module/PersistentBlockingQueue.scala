package com.bwsw.sj.common.module


import java.nio.file.{Files, Path}
import java.util.concurrent.locks._

import net.openhft.chronicle.queue.ChronicleQueueBuilder
import java.util.concurrent.TimeUnit._
/**
 * Provides a blocking queue to keep Transactions from sub. consumers and than pass them into module
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

  /**
   * Puts a message in a queue with blocking
   * @param message Specific data
   */
  def put(message: String) = {
    mutex.lock()
    writer.writeText(message)
    cond.signal()
    mutex.unlock()
  }

  /**
   * Queue is blocked on idle timeout (in milliseconds) after which null will be returned
   * @param idle Timeout of waiting
   * @return Received message or null
   */
  def get(idle: Long) = {
    mutex.lock()
    var data = reader.readText()
    if (data == null) {
      cond.await(idle, MILLISECONDS)
      data = reader.readText()
    }
    mutex.unlock()
    data
  }
}
