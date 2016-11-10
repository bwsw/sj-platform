package com.bwsw.sj.engine.core.engine

import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit._
import java.util.concurrent.locks._

import net.openhft.chronicle.queue.ChronicleQueueBuilder
import org.slf4j.LoggerFactory

/**
 * Provides a blocking queue to keep incoming envelopes that are serialized into a string,
 * which will be retrieved inside an engine
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
   * Put a message in a queue using blocking
   */
  def put(message: String) = {
    logger.info(s"Put a message: $message to queue\n")
    mutex.lock()
    writer.writeText(message)
    cond.signal()
    mutex.unlock()
  }

  /**
   * Queue is blocked on idle timeout (in milliseconds) after which Option will be returned
   * @param idle Timeout of waiting
   * @return Some received message or None
   */
  def get(idle: Long): Option[String] = {
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
    Option(data)
  }
}
