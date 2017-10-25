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
package com.bwsw.sj.engine.core.testutils.benchmark.loader.tstreams

import com.bwsw.sj.common.utils.benchmark.TStreamsDataSender
import com.bwsw.sj.engine.core.testutils.benchmark.loader.BenchmarkDataSender
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}

/**
  * Provides methods for sending data into T-Streams stream for test some application
  *
  * @author Pavel Tomskikh
  */
class TStreamsBenchmarkDataSender(config: TStreamsBenchmarkDataLoaderConfig)
  extends BenchmarkDataSender[TStreamsBenchmarkDataSenderParameters] {

  override val warmingUpParameters = TStreamsBenchmarkDataSenderParameters(1, 10, 10)
  private val tStreamsFactory = new TStreamsFactory
  tStreamsFactory.setProperty(ConfigurationOptions.Coordination.endpoints, config.zooKeeperAddress)
  tStreamsFactory.setProperty(ConfigurationOptions.Common.authenticationKey, config.token)
  tStreamsFactory.setProperty(ConfigurationOptions.Stream.partitionsCount, 1)
  tStreamsFactory.setProperty(ConfigurationOptions.Coordination.path, config.prefix)

  private val client = tStreamsFactory.getStorageClient()
  private val sender = new TStreamsDataSender(
    config.zooKeeperAddress,
    config.stream,
    config.token,
    config.prefix,
    config.words,
    " ")

  private var currentStorageSize: Long = 0
  private var currentMessageSize: Long = 0
  private var currentTransactionSize: Long = 0

  clearStorage()

  /**
    * Removes data from T-Streams stream
    */
  override def clearStorage(): Unit =
    client.deleteStream(config.stream)

  /**
    * Sends data into T-Streams stream
    *
    * @param parameters sending data parameters
    */
  override def send(parameters: TStreamsBenchmarkDataSenderParameters): Unit = {
    if (currentTransactionSize != parameters.transactionSize || currentMessageSize != parameters.messageSize) {
      clearStorage()
      currentStorageSize = 0
      currentTransactionSize = parameters.transactionSize
      currentMessageSize = parameters.messageSize
    }

    if (parameters.messagesCount > currentStorageSize) {
      val appendedMessages = parameters.messagesCount - currentStorageSize
      sender.send(currentMessageSize, appendedMessages + appendedMessages / 10, currentTransactionSize)
      currentStorageSize = parameters.messagesCount
    }
  }

  override def iterator: Iterator[TStreamsBenchmarkDataSenderParameters] = {
    config.sizePerTransaction.flatMap { transactionSize =>
      config.messageSizes.flatMap { messageSize =>
        config.messagesCounts.map { messagesCount =>
          TStreamsBenchmarkDataSenderParameters(transactionSize, messageSize, messagesCount)
        }
      }
    }.iterator
  }

  /**
    * Closes connections with T-Streams
    */
  override def stop(): Unit = {
    client.shutdown()
    tStreamsFactory.close()
  }
}
