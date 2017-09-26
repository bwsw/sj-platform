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
package com.bwsw.sj.engine.input.task

import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}

import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.input.task.reporting.InputStreamingPerformanceMetricsThread
import com.bwsw.tstreams.agents.producer.{NewProducerTransactionPolicy, ProducerTransaction}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

/**
  * Thread for sending data to the T-Streams service
  *
  * @param manager            allows to manage an environment of input streaming task
  * @param performanceMetrics set of metrics that characterize performance of an input streaming module
  * @author Pavel Tomskikh
  */
class SenderThread(manager: InputTaskManager,
                   performanceMetrics: InputStreamingPerformanceMetricsThread)
  extends Thread(s"input-task-${manager.taskName}-sender") {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val producers = manager.outputProducers
  private val checkpointGroup = manager.createCheckpointGroup()
  private val transactionsByStreamPartitions = createTransactionsStorage()
  private val messagesQueue: ArrayBlockingQueue[Message] = new ArrayBlockingQueue[Message](EngineLiterals.queueSize)

  override def run(): Unit = {
    addProducersToCheckpointGroup()

    while (true) {
      messagesQueue.poll(EngineLiterals.eventWaitTimeout, TimeUnit.MILLISECONDS) match {
        case SerializedEnvelope(bytes, outputMetadata) =>
          outputMetadata.foreach { case (stream, partition) => sendData(bytes, stream, partition) }
        case Checkpoint => doCheckpoint()
        case _ =>
      }
    }
  }

  /**
    * Sends serialized envelope to output streams
    */
  def send(envelope: SerializedEnvelope): Unit =
    messagesQueue.put(envelope)

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
    logger.debug(s"Task name: ${manager.taskName}. Create a storage for keeping txns for each partition of output streams.")

    streams.map(stream => (stream, mutable.Map[Int, ProducerTransaction]())).toMap
  }

  /**
    * Clears a map that keeps current open txn for each partition of output stream
    */
  private def clearTransactionStorage(): Unit = {
    logger.debug(s"Task name: ${manager.taskName}. Clear a storage for keeping txns for each partition of output streams.")
    transactionsByStreamPartitions.values.foreach(_.clear())
  }

  private def addProducersToCheckpointGroup(): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Start adding t-stream producers to checkpoint group.")
    producers.foreach(x => checkpointGroup.add(x._2))
    logger.debug(s"Task: ${manager.taskName}. The t-stream producers are added to checkpoint group.")
  }

  /**
    * Sends an input envelope data to output stream
    *
    * @param bytes     serialized input envelope
    * @param stream    output stream
    * @param partition partition of a stream
    */
  private def sendData(bytes: Array[Byte], stream: String, partition: Int): Unit = {
    logger.debug(s"Task name: ${manager.taskName}. Send envelope to output stream/partition '$stream/$partition'.")
    val transaction = transactionsByStreamPartitions(stream).getOrElseUpdate(
      partition,
      producers(stream).newTransaction(NewProducerTransactionPolicy.ErrorIfOpened, partition))
    transaction.send(bytes)

    logger.debug(s"Task name: ${manager.taskName}. Add envelope to output stream in performance metrics .")
    performanceMetrics.addElementToOutputEnvelope(
      stream,
      transaction.getTransactionID.toString,
      bytes.length)
  }

  private def doCheckpoint(): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Do group checkpoint.")
    checkpointGroup.checkpoint()
    synchronized {
      notify()
    }
    clearTransactionStorage()
  }
}
