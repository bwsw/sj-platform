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
package com.bwsw.sj.engine.core.testutils.benchmark.loader

/**
  * Provides methods for sending data into some storage for test some application.
  * Subclasses must iterate through all combination of sending data parameters.
  *
  * @tparam T type of storage parameters
  * @author Pavel Tomskikh
  */
trait BenchmarkDataSender[T <: BenchmarkDataSenderParameters] extends Iterable[T] {

  val warmingUpParameters: T

  /**
    * Sends data into storage for first test
    */
  def warmUp(): Unit =
    send(warmingUpParameters)

  /**
    * Sends data into storage
    *
    * @param parameters sending data parameters
    */
  def send(parameters: T): Unit

  /**
    * Removes data from storage
    */
  def clearStorage(): Unit

  /**
    * Closes connections with storage
    */
  def stop(): Unit
}
