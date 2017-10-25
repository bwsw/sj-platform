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
package com.bwsw.sj.stubs.module.batch_streaming

import java.net.Socket

import com.bwsw.sj.common.engine.core.entities.{KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.common.engine.core.state.StateStorage
import com.bwsw.sj.common.engine.core.batch.{BatchStreamingExecutor, WindowRepository}

import scala.util.{Random, Try}


class Executor(manager: ModuleEnvironmentManager) extends BatchStreamingExecutor[Integer](manager) {
  val state: StateStorage = manager.getState
  val splitOptions = manager.options.split(",")
  val totalInputElements = splitOptions(0).toInt
  val benchmarkPort = splitOptions(1).toInt

  val sumStateName = "sum"
  val elementsStateName = "elements"

  override def onInit(): Unit = {
    if (!state.isExist(sumStateName)) state.set(sumStateName, 0)
    if (!state.isExist(elementsStateName)) state.set(elementsStateName, 0)
    println("new init")
  }

  override def onWindow(windowRepository: WindowRepository): Unit = {
    val outputs = manager.getStreamsByTags(Array("output"))
    val output = manager.getRoundRobinOutput(outputs(Random.nextInt(outputs.length)))
    var sum = state.get(sumStateName).asInstanceOf[Int]
    var elements = state.get(elementsStateName).asInstanceOf[Int]
    val allWindows = windowRepository.getAll()

    if (Random.nextInt(100) < 5) {
      println("it happened")
      throw new Exception("it happened")
    }

    val allBatches =
      if (sum == 0) allWindows.flatMap(_._2.batches)
      else allWindows.flatMap(_._2.batches.takeRight(windowRepository.slidingInterval))

    allBatches.foreach(x => {
      output.put(x)
      println("stream name = " + x.stream)
    })

    allBatches.flatMap(_.envelopes).foreach {
      case kafkaEnvelope: KafkaEnvelope[Integer@unchecked] =>
        sum += kafkaEnvelope.data
        elements += 1
        state.set(sumStateName, sum)
        state.set(elementsStateName, elements)

      case tstreamEnvelope: TStreamEnvelope[Integer@unchecked] =>
        tstreamEnvelope.data.foreach(x => {
          sum += x
          elements += 1
        })
        state.set(sumStateName, sum)
        state.set(elementsStateName, elements)
    }
  }

  override def onTimer(jitter: Long): Unit = {
    println("onTimer")
  }

  override def onBeforeCheckpoint(): Unit = {
    println("on before checkpoint")
  }

  override def onIdle(): Unit = {
    println("on Idle")
  }

  /**
    * Handler triggered before save state
    *
    * @param isFullState Flag denotes that full state (true) or partial changes of state (false) will be saved
    */
  override def onBeforeStateSave(isFullState: Boolean): Unit = {
    println("on before state saving")

    val elements = state.get(elementsStateName).asInstanceOf[Int]
    if (elements >= totalInputElements && benchmarkPort > 0)
      Try(new Socket("localhost", benchmarkPort))
  }

  override def onEnter() = {
    println("on enter")
  }

  override def onLeaderEnter() = {
    println("on leader enter")
  }
}
