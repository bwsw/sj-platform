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
package com.bwsw.sj.test.module.batch

import com.bwsw.sj.common.engine.core.batch.{BatchStreamingExecutor, WindowRepository}
import com.bwsw.sj.common.engine.core.entities.{KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.engine.core.environment.ModuleEnvironmentManager
import com.typesafe.scalalogging.Logger

/**
  * @author Pavel Tomskikh
  */
class Executor(manager: ModuleEnvironmentManager) extends BatchStreamingExecutor[Integer](manager) {
  private val logger = Logger(getClass)
  private val outputStreams = manager.outputs.map(_.name).toSeq
  private val state = manager.getState
  private val sumStateName = "sum"

  override def onInit(): Unit = {
    logger.debug("init")
    if (!state.isExist(sumStateName)) state.set(sumStateName, 0)
  }

  override def onWindow(windowRepository: WindowRepository): Unit = {
    val outputs = outputStreams.map(manager.getRoundRobinOutput)
    var sum = state.get(sumStateName).asInstanceOf[Int]
    val allWindows = windowRepository.getAll().values

    val batches =
      if (sum == 0) allWindows.flatMap(_.batches)
      else allWindows.flatMap(_.batches.takeRight(windowRepository.slidingInterval))

    batches.foreach(_.envelopes.foreach {
      case kafkaEnvelope: KafkaEnvelope[Integer@unchecked] =>
        logger.debug(s"Got Kafka envelope ${kafkaEnvelope.id} with element ${kafkaEnvelope.data}")
        sum += kafkaEnvelope.data

      case tStreamsEnvelope: TStreamEnvelope[Integer@unchecked] =>
        logger.debug(
          s"Got T-Streams envelope ${tStreamsEnvelope.id} with elements ${tStreamsEnvelope.data.toList.mkString(", ")}")
        sum += tStreamsEnvelope.data.map(Int.unbox).sum
    })

    state.set(sumStateName, sum)

    outputs.foreach(_.put(Int.box(sum)))
  }
}
