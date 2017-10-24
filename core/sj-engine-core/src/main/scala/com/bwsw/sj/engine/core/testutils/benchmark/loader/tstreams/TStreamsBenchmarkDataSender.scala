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

  private val warmingUpTransactionSize: Long = 10
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

  /**
    * Sends data into T-Streams stream for first test
    */
  override def warmUp(): Unit = {
    clearStorage()
    sender.send(warmingUpMessageSize, warmingUpMessagesCount, warmingUpTransactionSize)
  }

  /**
    * Removes data from T-Streams stream
    */
  override def clearStorage(): Unit =
    client.deleteStream(config.stream)

  override def iterator: Iterator[TStreamsBenchmarkDataSenderParameters] = {
    new Iterator[TStreamsBenchmarkDataSenderParameters] {

      private val transactionSizeIterator = config.sizePerTransaction.iterator
      private var messageSizeIterator = config.messageSizes.iterator
      private var messagesCountIterator = config.messagesCounts.iterator
      private var transactionSize = transactionSizeIterator.next()
      private var messageSize = messageSizeIterator.next()
      private var currentTopicSize: Long = 0

      override def hasNext: Boolean =
        transactionSizeIterator.hasNext || messageSizeIterator.hasNext || messagesCountIterator.hasNext

      override def next(): TStreamsBenchmarkDataSenderParameters = {
        if (messagesCountIterator.isEmpty) {
          if (messageSizeIterator.isEmpty) {
            transactionSize = transactionSizeIterator.next()
            messageSizeIterator = config.messageSizes.iterator
          }

          messageSize = messageSizeIterator.next()
          messagesCountIterator = config.messagesCounts.iterator
          clearStorage()
          currentTopicSize = 0
        }

        val messagesCount = messagesCountIterator.next()
        if (messagesCount > currentTopicSize) {
          val appendedMessages = messagesCount - currentTopicSize
          sender.send(messageSize, appendedMessages + appendedMessages / 10, transactionSize)
          currentTopicSize = messagesCount
        }

        TStreamsBenchmarkDataSenderParameters(transactionSize, messageSize, messagesCount)
      }
    }
  }

  /**
    * Closes connections with T-Streams
    */
  override def stop(): Unit = {
    client.shutdown()
    tStreamsFactory.close()
  }
}
