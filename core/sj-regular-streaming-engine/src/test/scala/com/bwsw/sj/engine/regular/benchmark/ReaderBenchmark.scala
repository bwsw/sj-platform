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

/**
  * Provides methods for testing the speed of reading data from storage by some application.
  *
  * @author Pavel Tomskikh
  */
trait ReaderBenchmark {

  protected val warmingUpMessageSize: Long = 10
  protected val warmingUpMessagesCount: Long = 10

  /**
    * Performs the first test because it needs more time than subsequent tests
    */
  def warmUp(): Long = runTest(warmingUpMessageSize, warmingUpMessagesCount)

  /**
    * Sends data into the Kafka server and runs an application under test
    *
    * @param messageSize   size of one message that is sent to the storage
    * @param messagesCount count of messages
    * @return time in milliseconds within which an application under test reads messages from storage
    */
  def runTest(messageSize: Long, messagesCount: Long): Long

  /**
    * Closes opened connections, deletes temporary files
    */
  def close(): Unit
}
