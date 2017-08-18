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
package com.bwsw.sj.engine.regular.benchmark.read_kafka.samza

import java.io.{File, FileWriter}

import com.bwsw.sj.engine.regular.benchmark.read_kafka.samza.SamzaBenchmarkLiterals._
import org.apache.samza.config.Config
import org.apache.samza.system.IncomingMessageEnvelope
import org.apache.samza.task._

/**
  * @author Pavel Tomskikh
  */
class SamzaBenchmarkStreamTask extends StreamTask with InitableTask {
  private var readMessages: Int = 0
  private var messagesCount: Long = 10
  private var outputFilename: String = "samza-benchmark-result"
  private var firstMessageTimestamp: Long = 0

  override def init(config: Config, context: TaskContext): Unit = {
    outputFilename = config.get(outputFileConfig)
    messagesCount = config.getLong(messagesCountConfig)
  }

  override def process(envelope: IncomingMessageEnvelope, collector: MessageCollector, coordinator: TaskCoordinator): Unit = {
    if (readMessages == 0)
      firstMessageTimestamp = System.currentTimeMillis()

    readMessages += 1

    if (readMessages == messagesCount) {
      val lastMessageTimestamp = System.currentTimeMillis()

      val outputFile = new File(outputFilename)
      val writer = new FileWriter(outputFile, true)
      writer.write(s"${lastMessageTimestamp - firstMessageTimestamp}\n")
      writer.close()
    }
  }
}
