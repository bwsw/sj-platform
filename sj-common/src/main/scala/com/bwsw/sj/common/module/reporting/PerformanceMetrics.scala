package com.bwsw.sj.common.module.reporting

/**
 * Class represents a set of metrics that characterize performance of module
 * Created: 07/06/2016
 * @author Kseniya Mikhaleva
 */

import java.util.concurrent.locks.ReentrantLock

import com.bwsw.common.JsonSerializer
import org.slf4j.LoggerFactory

import scala.collection._
import scala.collection.mutable.ListBuffer

abstract class PerformanceMetrics(taskId: String, host: String, inputStreamNames: Array[String], outputStreamNames: Array[String]) {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected val mutex: ReentrantLock
  protected val serializer = new JsonSerializer()
  protected val startTime = System.currentTimeMillis()
  protected var inputEnvelopesPerStream = mutable.Map(inputStreamNames.map(x => (x, mutable.ListBuffer[List[Int]]())): _*)
  protected var outputEnvelopesPerStream = mutable.Map(outputStreamNames.map(x => (x, mutable.Map[String, mutable.ListBuffer[Int]]())): _*)

  /**
   * Constructs a report of performance metrics of task's work
   * @return Constructed performance report
   */
  def getReport: String

  /**
   * Invokes when a new envelope from some input stream is received
   * @param name Stream name
   * @param elementsSize Set of sizes of elements
   */
  def addEnvelopeToInputStream(name: String, elementsSize: List[Int]) = {
    mutex.lock()
    logger.debug(s"Indicate that a new envelope is received from input stream: $name\n")
    if (inputEnvelopesPerStream.contains(name)) {
      inputEnvelopesPerStream(name) += elementsSize
    } else {
      logger.error(s"Input stream with name: $name doesn't exist\n")
      throw new Exception(s"Input stream with name: $name doesn't exist")
    }
    mutex.unlock()
  }

  /**
   * Invokes when a new element is sent to txn of some output stream
   * @param name Stream name
   * @param envelopeID Id of envelope of output stream
   * @param elementSize Size of appended element
   */
  def addElementToOutputEnvelope(name: String, envelopeID: String, elementSize: Int) = {
    mutex.lock()
    logger.debug(s"Indicate that a new element is sent to txn: $envelopeID of output stream: $name\n")
    if (outputEnvelopesPerStream.contains(name)) {
      if (outputEnvelopesPerStream(name).contains(envelopeID)) {
        outputEnvelopesPerStream(name)(envelopeID) += elementSize
      } else {
        logger.debug(s"Output stream with name: $name doesn't contain txn: $envelopeID\n")
        outputEnvelopesPerStream(name) += (envelopeID -> ListBuffer(elementSize))
      }
    } else {
      logger.error(s"Output stream with name: $name doesn't exist\n")
      throw new Exception(s"Output stream with name: $name doesn't exist")
    }
    mutex.unlock()
  }
}