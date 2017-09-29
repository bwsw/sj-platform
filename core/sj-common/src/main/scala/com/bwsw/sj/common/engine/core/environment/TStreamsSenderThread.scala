/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.common.engine.core.environment

import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}

import com.bwsw.sj.common.engine.core.reporting.PerformanceMetricsProxy
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.{NewProducerTransactionPolicy, Producer, ProducerTransaction}
import com.typesafe.scalalogging.Logger

import scala.collection.mutable

/**
  * Thread for sending data to the T-Streams service
  *
  * @param producers          t-stream producers for saving data
  * @param checkpointGroup    group of t-stream agents that have to make a checkpoint at the same time
  * @param performanceMetrics set of metrics that characterize performance of an input streaming module
  * @param threadName         name of new thread
  * @author Pavel Tomskikh
  */
class TStreamsSenderThread(producers: Map[String, Producer],
                           checkpointGroup: CheckpointGroup,
                           performanceMetrics: PerformanceMetricsProxy,
                           threadName: String)
  extends Thread(threadName) {

  import TStreamsSenderThread._

  private val logger: Logger = Logger(this.getClass)
  private val transactionsByStreamPartitions = createTransactionsStorage()
  private val messagesQueue: ArrayBlockingQueue[Message] = new ArrayBlockingQueue[Message](EngineLiterals.queueSize)

  override def run(): Unit = {
    while (true) {
      messagesQueue.poll(EngineLiterals.eventWaitTimeout, TimeUnit.MILLISECONDS) match {
        case SerializedEnvelope(bytes, stream, partition) => internalSend(bytes, stream, partition)
        case Checkpoint => internalCheckpoint()
        case _ =>
      }
    }
  }

  /**
    * Sends bytes to output stream
    */
  def send(bytes: Array[Byte], stream: String, partition: Int = -1): Unit =
    messagesQueue.put(SerializedEnvelope(bytes, stream, partition))

  /**
    * Performs checkpoint
    */
  def checkpoint(): Unit = {
    synchronized {
      messagesQueue.put(Checkpoint)
      wait()
    }
  }


  /**
    * Creates a map that keeps current open txn for each partition of output stream
    *
    * @return map where a key is output stream name, a value is a map
    *         in which key is a number of partition and value is a txn
    */
  private def createTransactionsStorage(): Map[String, mutable.Map[Int, ProducerTransaction]] = {
    val streams = producers.keySet
    logger.debug(s"Thread $threadName. Create a storage for keeping txns for each partition of output streams.")

    streams.map(stream => (stream, mutable.Map[Int, ProducerTransaction]())).toMap
  }

  /**
    * Clears a map that keeps current open txn for each partition of output stream
    */
  private def clearTransactionStorage(): Unit = {
    logger.debug(s"Thread $threadName. Clear a storage for keeping txns for each partition of output streams.")
    transactionsByStreamPartitions.values.foreach(_.clear())
  }

  /**
    * Sends an input envelope data to output stream
    *
    * @param bytes     serialized input envelope
    * @param stream    output stream
    * @param partition partition of a stream
    */
  private def internalSend(bytes: Array[Byte], stream: String, partition: Int): Unit = {
    logger.debug(s"Thread $threadName. Send envelope to output stream/partition '$stream/$partition'.")
    val transaction = transactionsByStreamPartitions(stream).getOrElseUpdate(
      partition,
      producers(stream).newTransaction(NewProducerTransactionPolicy.ErrorIfOpened, partition))
    transaction.send(bytes)

    logger.debug(s"Thread $threadName. Add envelope to output stream in performance metrics .")
    performanceMetrics.addElementToOutputEnvelope(
      stream,
      transaction.getTransactionID.toString,
      bytes.length)
  }

  private def internalCheckpoint(): Unit = {
    logger.debug(s"Thread $threadName. Do group checkpoint.")
    checkpointGroup.checkpoint()
    synchronized {
      notify()
    }
    clearTransactionStorage()
  }
}

object TStreamsSenderThread {

  /**
    * Message for a [[TStreamsSenderThread]]
    *
    * @author Pavel Tomskikh
    */
  trait Message

  /**
    * Contains serialized envelope data and information about output streams
    *
    * @param bytes serialized data
    */
  case class SerializedEnvelope(bytes: Array[Byte], stream: String, partition: Int) extends Message

  /**
    * Tells that a [[TStreamsSenderThread]] must perform checkpoint
    */
  case object Checkpoint extends Message

}
