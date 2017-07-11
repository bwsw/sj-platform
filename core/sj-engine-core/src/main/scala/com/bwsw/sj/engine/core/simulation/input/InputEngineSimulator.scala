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
  * Imitates behavior of InputTaskEngine for testing an
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
  * simulator.prepare(Seq("1", "2", "a", "3", "b")) // byte buffer in simulator will be contatins "1,2,a,3,b,"
  * val outputDataList = simulator.process(duplicateCheck = true)
  * println(outputDataList)
  * }}}
  *
  * @param executor       implementation of [[InputStreamingExecutor]] under test
  * @param evictionPolicy eviction policy of duplicate envelopes
  * @param separator      delimiter between data records
  * @param charset        encoding of incoming data
  * @tparam T type of outgoing data
  * @author Pavel Tomskikh
  */
class InputEngineSimulator[T <: AnyRef](executor: InputStreamingExecutor[T],
                                        evictionPolicy: InputInstanceEvictionPolicy,
                                        separator: String = "",
                                        charset: Charset = Charset.forName("UTF-8")) {

  private val inputBuffer: ByteBuf = Unpooled.buffer()

  /**
    * Write data records in byte buffer
    *
    * @param records incoming data records
    */
  def prepare(records: Seq[String]): Unit = records.foreach(prepare)

  /**
    * Write data record in byte buffer
    *
    * @param record incoming data record
    */
  def prepare(record: String): Unit =
    inputBuffer.writeCharSequence(record + separator, charset)

  /**
    * Sends byte buffer to [[executor]] as long as it can tokenize the buffer. Method returns list of [[OutputData]].
    *
    * @param duplicateCheck indicates that every envelope has to be checked on duplication
    * @param clearBuffer    indicates that byte buffer must be cleared
    * @return list of [[OutputData]]
    */
  def process(duplicateCheck: Boolean, clearBuffer: Boolean = true): Seq[OutputData[T]] = {
    def processOneInterval(outputDataList: Seq[OutputData[T]]): Seq[OutputData[T]] = {
      val maybeOutputData = executor.tokenize(inputBuffer).map {
        interval =>
          val maybeInputEnvelope = executor.parse(inputBuffer, interval)
          val isNotDuplicate = maybeInputEnvelope.map(x => !isDuplicate(duplicateCheck)(x))
          val response = executor.createProcessedMessageResponse(maybeInputEnvelope, isNotDuplicate.getOrElse(false))
          inputBuffer.readerIndex(interval.finalValue + 1)
          inputBuffer.discardReadBytes()

          OutputData(maybeInputEnvelope, isNotDuplicate, response)
      }

      maybeOutputData match {
        case Some(outputData) => processOneInterval(outputDataList :+ outputData)
        case None => outputDataList
      }
    }

    val outputDataList = processOneInterval(Seq.empty)
    if (clearBuffer) clear()
    outputDataList
  }

  /**
    * Removes all data from byte buffer
    */
  def clear(): Unit = inputBuffer.clear()

  private def isDuplicate(duplicateCheck: Boolean)(inputEnvelope: InputEnvelope[T]): Boolean = {
    if (inputEnvelope.duplicateCheck.isDefined) {
      if (inputEnvelope.duplicateCheck.get) evictionPolicy.isDuplicate(inputEnvelope.key) else false
    } else {
      if (duplicateCheck) evictionPolicy.isDuplicate(inputEnvelope.key) else false
    }
  }
}

/**
  * Contains data from outputs of an [[InputStreamingExecutor]]
  *
  * @param inputEnvelope  result of [[InputStreamingExecutor.parse]]
  * @param isNotDuplicate indicates that [[inputEnvelope]] is not duplicate if [[inputEnvelope.isDefined]] or None 
  *                       otherwise
  * @param response       response that will be sent to a client after an [[inputEnvelope]] has been processed
  * @tparam T type of outgoing data
  */
case class OutputData[T <: AnyRef](inputEnvelope: Option[InputEnvelope[T]],
                                   isNotDuplicate: Option[Boolean],
                                   response: InputStreamingResponse)
