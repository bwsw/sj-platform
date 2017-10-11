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
package com.bwsw.sj.engine.core.testutils.benchmark.regular

import com.bwsw.sj.engine.core.testutils.benchmark.ReaderBenchmark

/**
  * Provides methods for testing the speed of reading data from storage by some application.
  *
  * @author Pavel Tomskikh
  */
trait RegularReaderBenchmark extends ReaderBenchmark {

  /**
    * Performs the first test because it needs more time than subsequent tests
    */
  def warmUp(): Long = {
    clearStorage()
    sendData(warmingUpMessageSize, warmingUpMessagesCount)
    runTest(warmingUpMessagesCount)
  }

  /**
    * Runs an application under test
    *
    * @param messagesCount count of messages
    * @return time in milliseconds within which an application under test reads messages from a storage
    */
  def runTest(messagesCount: Long): Long =
    awaitResult(runProcess(messagesCount))


  /**
    * Used to run an application under test in a separate process
    *
    * @param messagesCount count of messages
    * @return process of an application under test
    */
  protected def runProcess(messagesCount: Long): Process
}
