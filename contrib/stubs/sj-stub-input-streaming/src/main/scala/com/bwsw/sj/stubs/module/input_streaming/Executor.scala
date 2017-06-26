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
package com.bwsw.sj.stubs.module.input_streaming

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.engine.core.entities.InputEnvelope
import com.bwsw.sj.common.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.common.engine.core.input.{InputStreamingExecutor, Interval}
import io.netty.buffer.ByteBuf

class Executor(manager: InputEnvironmentManager) extends InputStreamingExecutor[String](manager) {
  val objectSerializer = new ObjectSerializer()
  val outputs = manager.getStreamsByTags(Array("output"))

  /**
   * Will be invoked every time when a new part of data is received
 *
   * @param buffer Input stream is a flow of bytes
   * @return Interval into buffer that probably contains a message or None
   */
  override def tokenize(buffer: ByteBuf): Option[Interval] = {
    val writeIndex = buffer.writerIndex()
    val endIndex = buffer.indexOf(0, writeIndex, 10)

    if (endIndex != -1) Some(Interval(0, endIndex)) else None
  }

  /**
   * Will be invoked after each calling tokenize method if tokenize doesn't return None
 *
   * @param buffer Input stream is a flow of bytes
   * @return Input envelope or None
   */
  override def parse(buffer: ByteBuf, interval: Interval) = {

    val rawData = buffer.slice(interval.initialValue, interval.finalValue)

    val data = new Array[Byte](rawData.capacity())
    rawData.getBytes(0, data)

    println("data into parse method " + new String(data) + ";")

    val envelope = new InputEnvelope(
      new String(data),
      outputs.map(x => (x, 0)),
      new String(data)
    )

    Some(envelope)
  }
}