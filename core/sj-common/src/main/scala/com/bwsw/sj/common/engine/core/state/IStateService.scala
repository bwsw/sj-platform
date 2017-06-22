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

import com.bwsw.sj.common.dal.model.instance._

/**
  * Trait represents a service to manage a state of module.
  * State will be saved from time to time.
  * How often a full state will be saved depends on [[RegularInstanceDomain.stateFullCheckpoint]]/[[BatchInstanceDomain.stateFullCheckpoint]].
  * This parameter indicating a number of checkponts. Until then a partial state will be saved (difference between previous and current state)
  *
  * @author Kseniya Mikhaleva
  */

trait IStateService {

  def isExist(key: String): Boolean

  def get(key: String): Any

  def getAll: Map[String, Any]

  def set(key: String, value: Any): Unit

  def delete(key: String): Unit

  def clear(): Unit

  /**
    * Indicates that a state variable has changed
    *
    * @param key   State variable name
    * @param value Value of the state variable
    */
  def setChange(key: String, value: Any): Unit

  /**
    * Indicates that a state variable has deleted
    *
    * @param key State variable name
    */
  def deleteChange(key: String): Unit

  /**
    * Indicates that all state variables have deleted
    */
  def clearChange(): Unit

  /**
    * Saves a partial state changes
    */
  def savePartialState(): Unit

  /**
    * Saves a state
    */
  def saveFullState(): Unit

  def getNumberOfVariables: Int
}
