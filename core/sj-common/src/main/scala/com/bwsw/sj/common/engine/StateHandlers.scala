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
package com.bwsw.sj.common.engine

/**
  * Provides methods for handle events (before and after state saving) in a stateful module
  */

trait StateHandlers {

  /**
    * Handler triggered before saving of the state
    *
    * @param isFullState flag denotes that the full state(true) or partial changes of state(false) is going to be saved
    */
  def onBeforeStateSave(isFullState: Boolean): Unit = {}

  /**
    * Handler triggered after saving of the state
    *
    * @param isFullState flag denotes that there was saved the full state(true) or partial changes of state(false)
    */
  def onAfterStateSave(isFullState: Boolean): Unit = {}
}
