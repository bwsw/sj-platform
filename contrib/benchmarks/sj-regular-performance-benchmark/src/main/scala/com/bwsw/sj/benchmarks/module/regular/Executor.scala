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
package com.bwsw.sj.benchmarks.module.regular

import java.io.{File, FileWriter}

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.benchmark.RegularExecutorOptions
import com.bwsw.sj.common.engine.core.entities.{KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.common.engine.core.regular.RegularStreamingExecutor

/**
  * Count milliseconds between receiving first and last messages and write it into a file. Options of instance should contain
  * a JSON with two fields:
  *
  * 1. outputFile - path to the file to write the result
  *
  * 2. messagesCount - count of input messages
  *
  * @author Pavel Tomskikh
  */
class Executor(manager: ModuleEnvironmentManager) extends RegularStreamingExecutor[String](manager) {
  private val jsonSerializer = new JsonSerializer(ignoreUnknown = true)
  private val options = jsonSerializer.deserialize[RegularExecutorOptions](manager.options)

  private var processedMessages: Long = 0
  private lazy val firstMessageTimestamp: Long = System.currentTimeMillis()
  private lazy val lastMessageTimestamp: Long = System.currentTimeMillis()


  override def onInit(): Unit = println("onInit")

  override def onMessage(envelope: KafkaEnvelope[String]): Unit = {
    if (processedMessages == 0) firstMessageTimestamp

    processedMessages += 1

    if (processedMessages == options.messagesCount) {
      lastMessageTimestamp

      val outputFile = new File(options.outputFilePath)
      val writer = new FileWriter(outputFile, true)
      writer.write(s"${lastMessageTimestamp - firstMessageTimestamp}\n")
      writer.close()
    }
  }

  override def onMessage(envelope: TStreamEnvelope[String]): Unit = {
    if (processedMessages == 0) firstMessageTimestamp

    processedMessages += envelope.data.toList.length

    if (processedMessages >= options.messagesCount) {
      lastMessageTimestamp

      val outputFile = new File(options.outputFilePath)
      val writer = new FileWriter(outputFile, true)
      writer.write(s"${lastMessageTimestamp - firstMessageTimestamp}\n")
      writer.close()
    }
  }

  override def deserialize(bytes: Array[Byte]): AnyRef = new String(bytes)
}
