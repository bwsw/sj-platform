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
package com.bwsw.sj.engine.regular.benchmark

import com.bwsw.sj.engine.core.testutils.TestStorageServer
import com.bwsw.sj.engine.core.testutils.benchmark.BenchmarkConfig
import com.bwsw.sj.engine.core.testutils.benchmark.loader.BenchmarkDataSenderConfig
import com.bwsw.sj.engine.core.testutils.benchmark.regular.RegularBenchmark
import com.bwsw.sj.engine.core.testutils.benchmark.sj.InputStreamFactory

/**
  * Provides methods for testing the speed of reading data from storage by Stream Juggler
  *
  * @param benchmarkConfig    configuration of application
  * @param senderConfig       configuration of input stream
  * @param inputStreamFactory used to create input stream
  * @param tStreamsPrefix     ZooKeeper root node which holds coordination tree
  * @param tStreamsToken      T-Streams authentication token
  * @author Pavel Tomskikh
  */
class SjRegularBenchmark(benchmarkConfig: BenchmarkConfig,
                         senderConfig: BenchmarkDataSenderConfig,
                         inputStreamFactory: InputStreamFactory,
                         tStreamsPrefix: Option[String] = None,
                         tStreamsToken: Option[String] = None)
  extends RegularBenchmark(benchmarkConfig) {

  private val helper = new SjRegularBenchmarkHelper(
    senderConfig.zooKeeperAddress,
    "benchmark",
    tStreamsPrefix.getOrElse(TestStorageServer.defaultPrefix),
    tStreamsToken.getOrElse(TestStorageServer.defaultToken),
    inputStreamFactory,
    tStreamsPrefix.isEmpty)

  override def start(): Unit =
    helper.start()

  override def stop(): Unit =
    helper.stop()


  override protected def runProcess(messagesCount: Long): Process =
    helper.runProcess(outputFile.getAbsolutePath, messagesCount)
}
