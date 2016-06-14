package com.bwsw.sj.engine.core

import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit._
import java.util.concurrent.locks._

import net.openhft.chronicle.queue.ChronicleQueueBuilder
import org.slf4j.LoggerFactory

/**
 * Provides a blocking queue to keep Transactions from sub. consumers and than pass them into module
 * @param path Temporary directory path for queue
 */
class PersistentBlockingQueue(path: String) {

  private val logger = LoggerFactory.getLogger(this.getClass)
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
    logger.info(s"Put a message: $message to queue\n")
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
    logger.info(s"Get next message from queue\n")
    mutex.lock()
    var data = reader.readText()
    if (data == null) {
      logger.debug(s"Waiting the message $idle milliseconds\n")
      cond.await(idle, MILLISECONDS)
      data = reader.readText()
      logger.debug(s"After $idle milliseconds message: $data was received\n")
    }
    mutex.unlock()
    data
  }
}
