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
package com.bwsw.sj.common.engine.core.state

/**
  * Class representing storage of state of module that can be used only in a stateful module.
  * State should be used to keep some global module variables related to processing
  *
  * @param stateService service for a state management
  * @author Kseniya Mikhaleva
  */

class StateStorage(stateService: IStateService) {

  def isExist(key: String): Boolean = {
    stateService.isExist(key)
  }

  def get(key: String): Any = {
    stateService.get(key)
  }

  def getAll: Map[String, Any] = {
    stateService.getAll
  }

  def set(key: String, value: Any): Unit = {
    stateService.set(key, value)
    stateService.setChange(key, value)
  }

  def delete(key: String): Unit = {
    stateService.delete(key)
    stateService.deleteChange(key)
  }

  def clear(): Unit = {
    stateService.clear()
    stateService.clearChange()
  }
}
