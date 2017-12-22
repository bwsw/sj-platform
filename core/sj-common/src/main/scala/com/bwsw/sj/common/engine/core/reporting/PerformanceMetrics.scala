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

import java.util.concurrent.{ArrayBlockingQueue, Callable, TimeUnit}

import com.bwsw.sj.common.engine.core.entities.{EnvelopeInterface, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.utils.EngineLiterals
import com.typesafe.scalalogging.Logger

/**
  * Handles data for [[PerformanceMetricsReporter]] in separate thread
  *
  * @author Pavel Tomskikh
  */
class PerformanceMetrics(performanceMetrics: PerformanceMetricsReporter, threadName: String) extends Callable[Unit] {

  import PerformanceMetrics._

  protected val logger: Logger = Logger(this.getClass)
  protected val envelopesQueue = new ArrayBlockingQueue[Message](EngineLiterals.queueSize)

  def addEnvelopeToInputStream(envelope: EnvelopeInterface): Unit = {
    envelope match {
      case tStreamEnvelope: TStreamEnvelope[_] =>
        addEnvelopeToInputStream(tStreamEnvelope.stream, tStreamEnvelope.data.toList)
      case kafkaEnvelope: KafkaEnvelope[_] =>
        addEnvelopeToInputStream(kafkaEnvelope.stream, List(kafkaEnvelope.data))
      case wrongEnvelope =>
        logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined")
        throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined")
    }
  }

  protected def addEnvelopeToInputStream(name: String, elements: List[AnyRef]): Unit =
    envelopesQueue.put(InputEnvelope(name, elements))

  /**
    * Invokes when a new element is sent to txn of some output stream
    *
    * @param name        stream name
    * @param envelopeID  id of envelope of output stream
    * @param elementSize size of appended element
    */
  def addElementToOutputEnvelope(name: String, envelopeID: String, elementSize: Long): Unit =
    envelopesQueue.put(OutputEnvelope(name, envelopeID, Right(elementSize)))

  /**
    * Invokes when a new element is sent to txn of some output stream
    *
    * @param name       stream name
    * @param envelopeID id of envelope of output stream
    * @param element    appended element
    */
  def addElementToOutputEnvelope(name: String, envelopeID: String, element: AnyRef): Unit =
    envelopesQueue.put(OutputEnvelope(name, envelopeID, Left(element)))

  override def call(): Unit = {
    while (true) {
      envelopesQueue.poll(EngineLiterals.eventWaitTimeout, TimeUnit.MILLISECONDS) match {
        case InputEnvelope(name, elements) =>
          performanceMetrics.addEnvelopeToInputStream(elements, name)
        case OutputEnvelope(name, envelopeID, Left(element)) =>
          performanceMetrics.addElementToOutputEnvelope(name, envelopeID, element)
        case OutputEnvelope(name, envelopeID, Right(elementSize)) =>
          performanceMetrics.addElementToOutputEnvelope(name, envelopeID, elementSize)
        case null =>
        case message => handleCustomMessage(message)
      }
    }
  }

  /**
    * Handles additional types of messages for inherited classes
    *
    * @param message message to be handle
    */
  protected def handleCustomMessage(message: Message): Unit = {}
}

object PerformanceMetrics {

  trait Message

  case class InputEnvelope(name: String, elements: List[AnyRef]) extends Message

  case class OutputEnvelope(name: String, envelopeID: String, element: Either[AnyRef, Long]) extends Message

}
