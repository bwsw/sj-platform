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
package com.bwsw.sj.engine.core.simulation.input

import java.nio.charset.Charset

import com.bwsw.sj.common.engine.core.entities.InputEnvelope
import com.bwsw.sj.common.engine.core.input.{InputStreamingExecutor, InputStreamingResponse}
import com.bwsw.sj.engine.input.eviction_policy.InputInstanceEvictionPolicy
import io.netty.buffer.{ByteBuf, Unpooled}

/**
  * Imitates behavior of [[com.bwsw.sj.engine.input.task.InputTaskEngine InputTaskEngine]] for testing an
  * implementation of [[InputStreamingExecutor]].
  *
  * Usage example:
  * {{{
  * val manager: InputEnvironmentManager
  * val executor = new SomeExecutor(manager)
  *
  * val hazelcastConfig = HazelcastConfig(600, 1, 1, EngineLiterals.lruDefaultEvictionPolicy, 100)
  * val hazelcast = new HazelcastMock(hazelcastConfig)
  * val evictionPolicy = InputInstanceEvictionPolicy(EngineLiterals.fixTimeEvictionPolicy, hazelcast)
  *
  * val simulator = new InputEngineSimulator(executor, evictionPolicy)
  * simulator.prepare("1,2,x,2,4")
  * val outputDatas = simulator.process(duplicateCheck = true)
  * println(outputDatas)
  * }}}
  *
  * @param executor       implementation of [[InputStreamingExecutor]] under test
  * @param evictionPolicy eviction policy of duplicate envelopes
  * @param charset        encoding of incoming data
  * @tparam T type of outgoing data
  * @author Pavel Tomskikh
  */
class InputEngineSimulator[T <: AnyRef](executor: InputStreamingExecutor[T],
                                        evictionPolicy: InputInstanceEvictionPolicy,
                                        charset: Charset = Charset.forName("UTF-8")) {

  private val inputBuffer: ByteBuf = Unpooled.buffer()

  /**
    * Write data in byte buffer
    *
    * @param data incoming data
    */
  def prepare(data: Seq[String]): Unit = data.foreach(prepare)

  /**
    * Write data in byte buffer
    *
    * @param data incoming data
    */
  def prepare(data: String): Unit =
    inputBuffer.writeCharSequence(data, charset)

  /**
    * Sends byte buffer to [[executor]] while it can tokenize buffer and returns output data
    *
    * @param duplicateCheck indicates that every envelope has to be checked has to be checked on duplication
    * @param clearBuffer    indicates that byte buffer must be cleared
    * @return collection of output data
    */
  def process(duplicateCheck: Boolean, clearBuffer: Boolean = true): Seq[OutputData[T]] = {
    def processOneInterval(outputDatas: Seq[OutputData[T]]): Seq[OutputData[T]] = {
      val maybeOutputData = executor.tokenize(inputBuffer).map {
        interval =>
          val maybeInputEnvelope = executor.parse(inputBuffer, interval)
          val isNotDuplicate = maybeInputEnvelope.map(checkDuplication(duplicateCheck))
          val response = executor.createProcessedMessageResponse(maybeInputEnvelope, isNotDuplicate.getOrElse(false))
          inputBuffer.readerIndex(interval.finalValue + 1)
          inputBuffer.discardReadBytes()

          OutputData(maybeInputEnvelope, isNotDuplicate, response)
      }

      maybeOutputData match {
        case Some(outputData) => processOneInterval(outputDatas :+ outputData)
        case None => outputDatas
      }
    }

    val outputDatas = processOneInterval(Seq.empty)
    if (clearBuffer) clear()
    outputDatas
  }

  /**
    * Removes all data from byte buffer
    */
  def clear(): Unit = inputBuffer.clear()

  private def checkDuplication(duplicateCheck: Boolean)(inputEnvelope: InputEnvelope[T]): Boolean = {
    if (inputEnvelope.duplicateCheck.isDefined) {
      if (inputEnvelope.duplicateCheck.get) evictionPolicy.checkForDuplication(inputEnvelope.key) else true
    } else {
      if (duplicateCheck) evictionPolicy.checkForDuplication(inputEnvelope.key) else true
    }
  }
}

/**
  * Contains data from outputs of an [[InputStreamingExecutor]]
  *
  * @param inputEnvelope  result of [[InputStreamingExecutor.parse]]
  * @param isNotDuplicate indicates that [[inputEnvelope]] is not duplicate
  * @param response       response that will be sent to a client after an [[inputEnvelope]] has been processed
  * @tparam T type of outgoing data
  */
case class OutputData[T <: AnyRef](inputEnvelope: Option[InputEnvelope[T]],
                                   isNotDuplicate: Option[Boolean],
                                   response: InputStreamingResponse)
