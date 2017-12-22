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
package com.bwsw.sj.common.engine.core.batch

import com.bwsw.sj.common.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.common.engine.{CheckpointHandlers, StateHandlers, StreamingExecutor, TimerHandlers}

/**
  * Class is responsible for batch module execution logic.
  * Module uses a specific instance to configure its work.
  * Executor provides the following methods, which don't do anything by default so you should define their implementation by yourself
  *
  * @author Kseniya Mikhaleva
  */


class BatchStreamingExecutor[T <: AnyRef](manager: ModuleEnvironmentManager)
  extends StreamingExecutor
    with StateHandlers
    with CheckpointHandlers
    with TimerHandlers {

  /**
    * Is invoked only once at the beginning of launching of module
    */
  def onInit(): Unit = {}

  /**
    * Used for processing one envelope. It is invoked for every received message
    * from one of the inputs that are defined within the instance.
    */
  def onWindow(windowRepository: WindowRepository): Unit = {}

  /**
    * Handler triggered if idle timeout goes out but a new message hasn't appeared.
    * Nothing to execute
    */
  def onIdle(): Unit = {}

  /**
    * system awaits when every task finishes onWindow method and then onEnter method of all tasks is invoked
    */
  def onEnter(): Unit = {}

  /**
    * system awaits when every task finishes onEnter method and then onLeaderEnter method of leader task is invoked
    */
  def onLeaderEnter(): Unit = {}
}