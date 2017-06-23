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

import com.bwsw.sj.common.engine.core.input.InputStreamingExecutor
import io.netty.buffer.{ByteBuf, Unpooled}

/**
  * @param executor implementation of [[InputStreamingExecutor]] under test
  * @param charset  encoding of incoming data
  * @tparam T type of outgoing data
  * @author Pavel Tomskikh
  */
class InputEngineSimulator[T <: AnyRef](executor: InputStreamingExecutor[T],
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
}
