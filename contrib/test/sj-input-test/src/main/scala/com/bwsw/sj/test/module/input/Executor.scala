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
package com.bwsw.sj.test.module.input

import com.bwsw.sj.common.engine.core.entities.InputEnvelope
import com.bwsw.sj.common.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.common.engine.core.input.utils.Tokenizer
import com.bwsw.sj.common.engine.core.input.{InputStreamingExecutor, Interval}
import com.typesafe.scalalogging.Logger
import io.netty.buffer.ByteBuf

import scala.util.Try

/**
  * @author Pavel Tomskikh
  */
class Executor(manager: InputEnvironmentManager) extends InputStreamingExecutor[Integer](manager) {
  private val logger = Logger(getClass)
  private val outputMetadata = manager.outputs.toSeq.map(stream => (stream.name, 0))
  private val tokenizer = new Tokenizer("\n", "UTF-8")

  override def tokenize(buffer: ByteBuf): Option[Interval] = tokenizer.tokenize(buffer)

  override def parse(buffer: ByteBuf, interval: Interval): Option[InputEnvelope[Integer]] = {
    val rawData = buffer.slice(interval.initialValue, interval.finalValue - interval.initialValue)

    val data = new Array[Byte](rawData.capacity())
    rawData.getBytes(0, data)
    val line = new String(data)

    logger.debug(s"Got line '$line'")

    Try(Int.box(line.toInt)).toOption
      .map(integer => InputEnvelope(line, outputMetadata, integer))
  }
}
