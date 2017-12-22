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

import java.net.Socket

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.stream.TStreamStreamDomain
import com.bwsw.sj.common.engine.core.entities.InputEnvelope
import com.bwsw.sj.common.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.common.engine.core.input.{InputStreamingExecutor, Interval}
import io.netty.buffer.ByteBuf

import scala.util.Random

class Executor(manager: InputEnvironmentManager) extends InputStreamingExecutor[String](manager) {
  private val serializer = new JsonSerializer(ignoreUnknown = true, enableNullForPrimitives = true)
  private val outputs = manager.outputs
    .filter(_.tags.contains("output"))
    .map(_.asInstanceOf[TStreamStreamDomain])
    .map(s => (s.name, s.partitions))

  private val options = serializer.deserialize[InputExecutorOptions](manager.options)
  private var inputElements = 0
  private val verbose = options.verbose.getOrElse(false)

  /**
    * Will be invoked every time when a new part of data is received
    *
    * @param buffer Input stream is a flow of bytes
    * @return Interval into buffer that probably contains a message or None
    */
  override def tokenize(buffer: ByteBuf): Option[Interval] = {
    val readerIndex = buffer.readerIndex()
    val writeIndex = buffer.writerIndex()
    val endIndex = buffer.indexOf(readerIndex, writeIndex, '\n')

    if (endIndex != -1) Some(Interval(readerIndex, endIndex)) else None
  }

  /**
    * Will be invoked after each calling tokenize method if tokenize doesn't return None
    *
    * @param buffer Input stream is a flow of bytes
    * @return Input envelope or None
    */
  override def parse(buffer: ByteBuf, interval: Interval) = {

    val rawData = buffer.slice(interval.initialValue, interval.finalValue - interval.initialValue)

    val data = new Array[Byte](rawData.capacity())
    rawData.getBytes(0, data)
    val line = new String(data)

    if (verbose)
      println("data into parse method " + line + ";")

    if (options.totalInputElements.isDefined) {
      inputElements += 1
      if (options.benchmarkPort.isDefined && inputElements == options.totalInputElements.get)
        new Socket("localhost", options.benchmarkPort.get)
    }

    Some(InputEnvelope(
      line,
      outputs.map { case (s, p) => (s, Random.nextInt(p)) },
      line))
  }
}