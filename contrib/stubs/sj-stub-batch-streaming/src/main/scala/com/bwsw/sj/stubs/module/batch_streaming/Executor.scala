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

import java.util.Random

import com.bwsw.sj.engine.core.entities.{KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.engine.core.state.StateStorage
import com.bwsw.sj.engine.core.batch.{WindowRepository, BatchStreamingExecutor}


class Executor(manager: ModuleEnvironmentManager) extends BatchStreamingExecutor[Integer](manager) {
  val state: StateStorage = manager.getState

  override def onInit(): Unit = {
    if (!state.isExist("sum")) state.set("sum", 0)
    println("new init")
  }

  override def onAfterCheckpoint(): Unit = {
    println("on after checkpoint")
  }

  override def onWindow(windowRepository: WindowRepository): Unit = {
    val outputs = manager.getStreamsByTags(Array("output"))
    val output = manager.getRoundRobinOutput(outputs(new Random().nextInt(outputs.length)))
    var sum = state.get("sum").asInstanceOf[Int]
    val allWindows = windowRepository.getAll()

    if (new Random().nextInt(100) < 5) {
      println("it happened")
      throw new Exception("it happened")
    }
    val begin = if (sum != 0) windowRepository.window - windowRepository.slidingInterval else 0
    val end = windowRepository.window

    allWindows.flatMap(x => x._2.batches.slice(begin, end)).foreach(x => {
      output.put(x)
      println("stream name = " + x.stream)
    })

    allWindows.flatMap(x => x._2.batches.slice(begin, end)).flatMap(x => x.envelopes).foreach {
      case kafkaEnvelope: KafkaEnvelope[Integer @unchecked] =>
        sum += kafkaEnvelope.data
        state.set("sum", sum)
      case tstreamEnvelope: TStreamEnvelope[Integer @unchecked] =>
        tstreamEnvelope.data.foreach(x => {
          sum += x
        })
        state.set("sum", sum)
    }
  }

  override def onTimer(jitter: Long): Unit = {
    println("onTimer")
  }

  override def onAfterStateSave(isFull: Boolean): Unit = {
    if (isFull) {
      println("on after full state saving")
    } else println("on after partial state saving")
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
  }

  override def onEnter() = {
    println("on enter")
  }

  override def onLeaderEnter() = {
    println("on leader enter")
  }

  override def onLeave() = {
    println("on leave")
  }

  override def onLeaderLeave() = {
     println("on leader leave")
  }
}