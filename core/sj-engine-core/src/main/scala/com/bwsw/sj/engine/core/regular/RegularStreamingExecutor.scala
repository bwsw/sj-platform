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
package com.bwsw.sj.engine.core.regular

import com.bwsw.sj.common.engine.{StateHandlers, StreamingExecutor}
import com.bwsw.sj.engine.core.entities.{KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager

/**
 * Class is responsible for regular module execution logic.
 * Module uses a specific instance to configure its work.
 * Executor provides the following methods, which don't do anything by default so you should define their implementation by yourself
 *
 * @author Kseniya Mikhaleva
 */

class RegularStreamingExecutor[T <: AnyRef](manager: ModuleEnvironmentManager) extends StreamingExecutor with StateHandlers {
  /**
   * Is invoked only once at the beginning of launching of module
   */
  def onInit(): Unit = {}

  /**
   * Used for processing one t-stream envelope. It is invoked for every received message
   * from one of the inputs that are defined within the instance.
   */
  def onMessage(envelope: TStreamEnvelope[T]): Unit = {}

  /**
    * Used for processing one kafka envelope. It is invoked for every received message
    * from one of the inputs that are defined within the instance.
    */
  def onMessage(envelope: KafkaEnvelope[T]): Unit = {}

  /**
   * Handler triggered before every checkpoint
   */
  def onBeforeCheckpoint(): Unit = {}

  /**
   * Handler triggered after every checkpoint
   */
  def onAfterCheckpoint(): Unit = {}

  /**
   * Is invoked every time when a set timer goes out
   *
   * @param jitter Delay between a real response time and an invocation of this handler
   */
  def onTimer(jitter: Long): Unit = {}

  /**
   * Handler triggered if idle timeout goes out but a new message hasn't appeared.
   * Nothing to execute
   */
  def onIdle(): Unit = {}
}