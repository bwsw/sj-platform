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
package com.bwsw.sj.test.module.regular

import com.bwsw.sj.common.engine.core.entities.{KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.common.engine.core.regular.RegularStreamingExecutor
import com.typesafe.scalalogging.Logger

/**
  * @author Pavel Tomskikh
  */
class Executor(manager: ModuleEnvironmentManager) extends RegularStreamingExecutor[Integer](manager) {
  private val logger = Logger(getClass)
  private val state = manager.getState
  private val outputStreams = manager.outputs.map(_.name)

  private val sumStateName = "sum"

  override def onInit(): Unit = {
    logger.debug("init")
    if (!state.isExist(sumStateName)) state.set(sumStateName, 0)
  }

  override def onMessage(envelope: TStreamEnvelope[Integer]): Unit = {
    logger.debug(s"Got T-Streams envelope ${envelope.id} with elements ${envelope.data.toList.mkString(", ")}")

    envelope.data.toList.foreach(i => increaseSum(i))
  }

  override def onMessage(envelope: KafkaEnvelope[Integer]): Unit = {
    logger.debug(s"Got Kafka envelope ${envelope.id} with element ${envelope.data}")

    increaseSum(envelope.data)
  }

  override def onBeforeCheckpoint(): Unit = {
    val outputs = outputStreams.map(manager.getRoundRobinOutput)
    val sum = getSum
    outputs.foreach(_.put(Int.box(sum)))
  }

  private def getSum: Int =
    state.get(sumStateName).asInstanceOf[Int]

  private def increaseSum(i: Int): Unit =
    state.set(sumStateName, getSum + i)
}
