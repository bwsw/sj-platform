package com.bwsw.sj.engine.core.testutils.benchmark

import java.io.{BufferedReader, File, FileReader}
import java.net.ServerSocket
import java.util.UUID

/**
  * Provides methods for testing the speed of reading data from storage by some application.
  *
  * @author Pavel Tomskikh
  */
trait ReaderBenchmark {

  protected val warmingUpMessageSize: Long = 10
  protected val warmingUpMessagesCount: Long = 10

  protected val lookupResultTimeout: Long = 5000
  protected val outputFilename = "benchmark-output-" + UUID.randomUUID().toString
  protected val outputFile = new File(outputFilename)

  /**
    * Performs the first test because it needs more time than subsequent tests
    */
  def warmUp(): Long

  /**
    * Generates data and send it to a storage
    *
    * @param messageSize   size of one message
    * @param messagesCount count of messages
    */
  def sendData(messageSize: Long, messagesCount: Long): Unit

  def clearStorage(): Unit

  /**
    * Closes opened connections, deletes temporary files
    */
  def stop(): Unit

  protected def awaitResult(process: Process): Long = {
    var maybeResult: Option[Long] = retrieveResultFromFile()
    while (maybeResult.isEmpty && process.isAlive) {
      Thread.sleep(lookupResultTimeout)
      maybeResult = retrieveResultFromFile()
    }

    if (process.isAlive)
      process.destroy()

    maybeResult.getOrElse(-1L)
  }

  /**
    * Retrieves result from file
    *
    * @return result if a file exists or None otherwise
    */
  private def retrieveResultFromFile(): Option[Long] = {
    if (outputFile.exists() && outputFile.length() > 0) {
      val reader = new BufferedReader(new FileReader(outputFile))
      val result = reader.readLine()
      reader.close()

      if (Option(result).exists(_.nonEmpty)) {
        outputFile.delete()

        Some(result.toLong)
      } else
        None
    }
    else
      None
  }

  protected def findFreePort(): Int = {
    val randomSocket = new ServerSocket(0)
    val port = randomSocket.getLocalPort
    randomSocket.close()

    port
  }
}
