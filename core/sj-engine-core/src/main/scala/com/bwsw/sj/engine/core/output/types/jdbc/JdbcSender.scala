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
package com.bwsw.sj.engine.core.output.types.jdbc

import com.bwsw.sj.common.engine.core.entities.{OutputEnvelope, TStreamEnvelope}

/**
  * Provides methods for sending data into a SQL database
  *
  * @tparam T incoming data type
  * @author Pavel Tomskikh
  */
trait JdbcSender[T <: AnyRef] {

  /**
    * Sends data into a SQL database
    *
    * @param envelope      processed envelope
    * @param inputEnvelope received envelope
    */
  def send(envelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[T]): Unit

  /**
    * Triggered before every checkpoint
    */
  def checkpoint(): Unit = {}
}

object JdbcSender {
  def apply[T <: AnyRef](commandBuilder: JdbcCommandBuilder, supportsBatchUpdates: Boolean): JdbcSender[T] = {
    if (supportsBatchUpdates) new BatchedJdbcSender(commandBuilder)
    else new SimpleJdbcSender(commandBuilder)
  }
}