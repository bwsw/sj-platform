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
  private val state: StateStorage = manager.getState
  private val splitOptions = manager.options.split(",")
  private val totalInputEnvelopes = splitOptions(0).toInt
  private val benchmarkPort = splitOptions(1).toInt

  private val sumStateName = "sum"
  private val envelopesStateName = "envelopes"

  override def onInit(): Unit = {
    if (!state.isExist(sumStateName)) state.set(sumStateName, 0)
    if (!state.isExist(envelopesStateName)) state.set(envelopesStateName, 0)
    println("new init")
  }

  override def onWindow(windowRepository: WindowRepository): Unit = {
    val outputs = manager.getStreamsByTags(Array("output"))
    val output = manager.getRoundRobinOutput(outputs(Random.nextInt(outputs.length)))
    var sum = state.get(sumStateName).asInstanceOf[Int]
    var envelopesCount = state.get(envelopesStateName).asInstanceOf[Int]
    val allWindows = windowRepository.getAll()
    val envelopesInWindow =
      windowRepository.window * windowRepository.slidingInterval * NumericalBatchCollector.batchSize * allWindows.size

    if (Random.nextInt(100) < 5) {
      println("it happened")
      throw new Exception("it happened")
    }

    val allBatches =
      if (envelopesCount + envelopesInWindow < totalInputEnvelopes)
        allWindows.flatMap(_._2.batches.take(windowRepository.slidingInterval))
      else
        allWindows.flatMap(_._2.batches)

    allBatches.foreach(x => {
      output.put(x)
      println("stream name = " + x.stream)
    })


    val envelopes = allBatches.flatMap(_.envelopes)
    envelopes.foreach {
      case kafkaEnvelope: KafkaEnvelope[Integer@unchecked] =>
        sum += kafkaEnvelope.data

      case tstreamEnvelope: TStreamEnvelope[Integer@unchecked] =>
        tstreamEnvelope.data.foreach(x => sum += x)
    }
    envelopesCount += envelopes.size

    state.set(sumStateName, sum)
    state.set(envelopesStateName, envelopesCount)
  }

  override def onTimer(jitter: Long): Unit = println("onTimer")

  override def onBeforeCheckpoint(): Unit = println("on before checkpoint")

  override def onIdle(): Unit = println("on Idle")

  /**
    * Handler triggered before save state
    *
    * @param isFullState Flag denotes that full state (true) or partial changes of state (false) will be saved
    */
  override def onBeforeStateSave(isFullState: Boolean): Unit = {
    println("on before state saving")

    val envelopes = state.get(envelopesStateName).asInstanceOf[Int]
    println(s"processed $envelopes/$totalInputEnvelopes envelopes")
    if (envelopes >= totalInputEnvelopes && benchmarkPort > 0)
      Try(new Socket("localhost", benchmarkPort))
  }

  override def onEnter(): Unit = println("on enter")

  override def onLeaderEnter(): Unit = println("on leader enter")
}
