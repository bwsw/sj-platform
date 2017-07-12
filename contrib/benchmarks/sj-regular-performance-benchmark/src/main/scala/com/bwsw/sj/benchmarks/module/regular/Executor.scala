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
import com.bwsw.sj.common.engine.core.entities.{KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.common.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.common.utils.BenchmarkLiterals.OptionsFieldNames

/**
  *
  * @author Pavel Tomskikh
  */
class Executor(manager: ModuleEnvironmentManager) extends RegularStreamingExecutor[String](manager) {
  private val jsonSerializer = new JsonSerializer(ignoreUnknown = true)
  private val optionsMap = jsonSerializer.deserialize[Map[String, Any]](manager.options)
  private val outputFilePath = optionsMap(OptionsFieldNames.outputFile).asInstanceOf[String]
  private val messagesCount: Long = optionsMap(OptionsFieldNames.messagesCount).asInstanceOf[Long]

  private var processedMessages: Long = 0
  private var firstMessageTimestamp: Long = 0
  private var lastMessageTimestamp: Long = 0


  override def onMessage(envelope: KafkaEnvelope[String]): Unit = stopWatcher()

  override def onMessage(envelope: TStreamEnvelope[String]): Unit = stopWatcher()


  private def stopWatcher(): Unit = {
    if (processedMessages == 0)
      firstMessageTimestamp = System.currentTimeMillis()

    processedMessages += 1

    if (processedMessages == messagesCount) {
      lastMessageTimestamp = System.currentTimeMillis()

      val outputFile = new File(outputFilePath)
      val writer = new FileWriter(outputFile)
      writer.write(s"${lastMessageTimestamp - firstMessageTimestamp}")
      writer.close()
    }
  }
}
