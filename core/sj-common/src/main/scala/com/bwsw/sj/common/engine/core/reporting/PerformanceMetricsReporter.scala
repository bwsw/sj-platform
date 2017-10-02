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
package com.bwsw.sj.common.engine.core.reporting

import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.{Callable, TimeUnit}

import com.bwsw.common.{JsonSerializer, ObjectSerializer, ObjectSizeFetcher}
import com.bwsw.sj.common.dal.model.stream.TStreamStreamDomain
import com.bwsw.sj.common.engine.core.entities.{EnvelopeInterface, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.engine.core.managment.TaskManager
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.tstreams.agents.producer.{NewProducerTransactionPolicy, Producer}
import com.typesafe.scalalogging.Logger

import scala.collection._
import scala.collection.mutable.ListBuffer


/**
  * Class is responsible for collecting performance metrics and save their via t-streams on every checkpoint
  *
  * @author Kseniya Mikhaleva
  */
abstract class PerformanceMetricsReporter(manager: TaskManager) extends Callable[Unit] {

  protected val currentThread: Thread = Thread.currentThread()
  protected val logger: Logger = Logger(this.getClass)
  protected val mutex: ReentrantLock = new ReentrantLock(true)
  protected val reportSerializer: JsonSerializer = new JsonSerializer()
  protected val startTime: Long = System.currentTimeMillis()
  protected var inputEnvelopesPerStream: mutable.Map[String, ListBuffer[List[Long]]]
  protected var outputEnvelopesPerStream: mutable.Map[String, mutable.Map[String, ListBuffer[Long]]]
  protected val taskName: String = manager.taskName
  protected val instance: Instance = manager.instance
  private val reportingInterval: Long = instance.performanceReportingInterval
  protected val report: PerformanceMetricsMetadata = new PerformanceMetricsMetadata()
  private val reportStreamName: String = instance.name + "_report"
  private val reportStream: TStreamStreamDomain = getReportStream()
  private val reportProducer: Producer = createReportProducer()

  fillStaticPerformanceMetrics()

  /**
    * Creates [[TStreamStreamDomain]] to keep the reports of module performance.
    * For each task there is specific partition (task number = partition number)
    */
  private def getReportStream(): TStreamStreamDomain = {
    logger.debug(s"Task name: $taskName. " +
      s"Get stream for performance metrics.")
    val tags = Array("report", "performance")
    val description = "store reports of performance metrics"
    val partitions = instance.countParallelism

    manager.createStorageStream(reportStreamName, description, partitions)

    manager.getStream(
      reportStreamName,
      description,
      tags,
      partitions
    )
  }

  /**
    * Create t-stream producer for stream for reporting
    */
  private def createReportProducer(): Producer = {
    logger.debug(s"Task: $taskName. Start creating a t-stream producer to record performance reports.")
    val reportProducer = manager.createProducer(reportStream)
    logger.debug(s"Task: $taskName. Creation of t-stream producer is finished.")

    reportProducer
  }

  private def fillStaticPerformanceMetrics(): Unit = {
    report.taskId = taskName
    report.host = manager.agentsHost
  }

  /**
    * It's in charge of cleaning a report of performance metrics for next time
    */
  protected def clear(): Unit

  def addEnvelopeToInputStream(envelope: EnvelopeInterface): Unit = {
    envelope match {
      case tStreamEnvelope: TStreamEnvelope[_] =>
        addEnvelopeToInputStream(tStreamEnvelope.stream, tStreamEnvelope.data.map(ObjectSizeFetcher.getObjectSize).toList)
      case kafkaEnvelope: KafkaEnvelope[_] =>
        addEnvelopeToInputStream(kafkaEnvelope.stream, List(ObjectSizeFetcher.getObjectSize(kafkaEnvelope.data)))
      case wrongEnvelope =>
        logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined")
        throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined")
    }
  }

  protected def addEnvelopeToInputStream(name: String, elementsSize: List[Long]): Unit = {
    mutex.lock()
    logger.debug(s"Indicate that a new envelope is received from input stream: $name.")
    if (inputEnvelopesPerStream.contains(name)) {
      inputEnvelopesPerStream(name) += elementsSize
    } else {
      logger.error(s"Input stream with name: $name doesn't exist.")
      throw new Exception(s"Input stream with name: $name doesn't exist")
    }
    mutex.unlock()
  }

  /**
    * Invokes when a new element is sent to txn of some output stream
    *
    * @param name        stream name
    * @param envelopeID  id of envelope of output stream
    * @param elementSize size of appended element
    */
  def addElementToOutputEnvelope(name: String, envelopeID: String, elementSize: Long): Unit = {
    mutex.lock()
    logger.debug(s"Indicate that a new element is sent to txn: $envelopeID of output stream: $name.")
    if (outputEnvelopesPerStream.contains(name)) {
      if (outputEnvelopesPerStream(name).contains(envelopeID)) {
        outputEnvelopesPerStream(name)(envelopeID) += elementSize
      } else {
        logger.debug(s"Output stream with name: $name doesn't contain txn: $envelopeID.")
        outputEnvelopesPerStream(name) += (envelopeID -> ListBuffer(elementSize))
      }
    } else {
      logger.error(s"Output stream with name: $name doesn't exist.")
      throw new Exception(s"Output stream with name: $name doesn't exist")
    }
    mutex.unlock()
  }

  /**
    * It is in charge of running of input module
    */
  override def call(): Unit = {
    logger.debug(s"Task: $taskName. Launch a new thread to report performance metrics .")
    val currentThread = Thread.currentThread()
    currentThread.setName(s"report-task-$taskName")

    while (true) {
      logger.info(s"Task: $taskName. Wait $reportingInterval ms to report performance metrics.")
      TimeUnit.MILLISECONDS.sleep(reportingInterval)
      val report = getReport()
      logger.info(s"Task: $taskName. Performance metrics: $report .")
      sendReport(report)
      logger.debug(s"Task: $taskName. Do checkpoint of producer for performance reporting.")
      reportProducer.checkpoint()
    }
  }

  /**
    * Constructs a report of performance metrics of task work (one module could have multiple tasks)
    */
  def getReport(): String

  private def sendReport(report: String): Unit = {
    val reportSerializerForTxn = new ObjectSerializer()
    val taskNumber = taskName.replace(s"${manager.instanceName}-task", "").toInt
    logger.debug(s"Task: $taskName. Create a new txn for sending performance metrics.")
    val reportTxn = reportProducer.newTransaction(NewProducerTransactionPolicy.ErrorIfOpened, taskNumber)
    logger.debug(s"Task: $taskName. Send performance metrics.")
    reportTxn.send(reportSerializerForTxn.serialize(report))
  }

  protected def createStorageForInputEnvelopes(inputStreamNames: Array[String]): mutable.Map[String, ListBuffer[List[Long]]] = {
    mutable.Map(inputStreamNames.map(x => (x, mutable.ListBuffer[List[Long]]())): _*)
  }

  protected def createStorageForOutputEnvelopes(outputStreamNames: Array[String]): mutable.Map[String, mutable.Map[String, ListBuffer[Long]]] = {
    mutable.Map(outputStreamNames.map(x => (x, mutable.Map[String, mutable.ListBuffer[Long]]())): _*)
  }
}