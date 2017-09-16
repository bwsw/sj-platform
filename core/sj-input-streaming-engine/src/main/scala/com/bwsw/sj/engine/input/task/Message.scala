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
package com.bwsw.sj.engine.input.task

/**
  * Message for a [[SenderThread]]
  *
  * @author Pavel Tomskikh
  */
trait Message

/**
  * Contains serialized envelope data and information about output streams
  *
  * @param bytes          serialized data
  * @param outputMetadata information about output streams
  */
case class SerializedEnvelope(bytes: Array[Byte], outputMetadata: Seq[(String, Int)]) extends Message

/**
  * Tells that a [[SenderThread]] must perform checkpoint
  */
case object Checkpoint extends Message
