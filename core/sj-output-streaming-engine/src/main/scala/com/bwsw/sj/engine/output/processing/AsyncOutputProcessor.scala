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
package com.bwsw.sj.engine.output.processing

import java.util.concurrent.Executors

import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.common.utils.EngineLiterals

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * [[com.bwsw.sj.engine.output.processing.OutputProcessor OutputProcessor]] that sends data between checkpoints
  * asynchronously
  *
  * @param outputStream       stream indicating the specific storage
  * @param performanceMetrics set of metrics that characterize performance of an output streaming module
  * @author Pavel Tomskikh
  */
abstract class AsyncOutputProcessor[T <: AnyRef](outputStream: StreamDomain,
                                                 performanceMetrics: PerformanceMetrics)
  extends OutputProcessor[T](outputStream, performanceMetrics) {

  private val futures = mutable.Queue.empty[Future[Unit]]
  private implicit val executionContext =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(EngineLiterals.outputProcessorExecutionContextSize))

  override def checkpoint(): Unit =
    futures.dequeueAll(_ => true).foreach(future => Await.result(future, Duration.Inf))

  override def send(envelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[T]): Unit =
    futures.enqueue(Future(asyncSend(envelope, inputEnvelope)))

  protected def asyncSend(envelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[T]): Unit

}
