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
package com.bwsw.sj.engine.batch.benchmark.read_kafka.storm

import java.io.{File, FileWriter}

import org.apache.storm.topology.base.BaseWindowedBolt
import org.apache.storm.windowing.TupleWindow

/**
  * @author Pavel Tomskikh
  */
class StormBenchmarkBolt(messagesCount: Long, outputFilename: String) extends BaseWindowedBolt {
  private var processedMessages: Long = 0
  private var firstMessageTimestamp: Long = _
  private var done: Boolean = false

  override def execute(inputWindow: TupleWindow): Unit = {
    if (processedMessages == 0)
      firstMessageTimestamp = System.currentTimeMillis()

    processedMessages += inputWindow.getNew.size()

    if (processedMessages >= messagesCount && !done) {
      val lastMessageTimestamp = System.currentTimeMillis()

      val outputFile = new File(outputFilename)
      val writer = new FileWriter(outputFile, true)
      writer.write(s"${lastMessageTimestamp - firstMessageTimestamp}\n")
      writer.close()

      done = true
    }
  }
}
