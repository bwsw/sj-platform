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
package com.bwsw.sj.examples.sum

import com.bwsw.sj.common.engine.core.batch.{BatchStreamingExecutor, WindowRepository}
import com.bwsw.sj.common.engine.core.entities.{KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.common.engine.core.state.StateStorage

import scala.util.Random


class Executor(manager: ModuleEnvironmentManager) extends BatchStreamingExecutor[Integer](manager) {
  private val outputs = manager.outputs.map(_.name)
  private val state: StateStorage = manager.getState
  private val sumStateName = "sum"
  private val elementsStateName = "elements"

  override def onInit(): Unit = {
    if (!state.isExist(sumStateName)) state.set(sumStateName, 0)
    if (!state.isExist(elementsStateName)) state.set(elementsStateName, 0)
    println("init")
  }

  override def onWindow(windowRepository: WindowRepository): Unit = {
    val output = manager.getRoundRobinOutput(outputs(Random.nextInt(outputs.length)))
    var sum = state.get(sumStateName).asInstanceOf[Int]
    var elements = state.get(elementsStateName).asInstanceOf[Int]
    val allWindows = windowRepository.getAll().values

    val batches =
      if (sum == 0) allWindows.flatMap(_.batches)
      else allWindows.flatMap(_.batches.takeRight(windowRepository.slidingInterval))

    batches.foreach(_.envelopes.foreach {
      case kafkaEnvelope: KafkaEnvelope[Integer@unchecked] =>
        sum += kafkaEnvelope.data
        elements += 1

      case tStreamEnvelope: TStreamEnvelope[Integer@unchecked] =>
        elements += tStreamEnvelope.data.length
        sum += tStreamEnvelope.data.map(Int.unbox).sum
    })

    state.set(sumStateName, sum)
    state.set(elementsStateName, elements)

    output.put((sum, elements))
  }
}