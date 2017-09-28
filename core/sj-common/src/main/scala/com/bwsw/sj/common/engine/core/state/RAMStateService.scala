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

import com.bwsw.sj.common.engine.core.state.StateLiterals.{deleteLiteral, setLiteral}
import com.typesafe.scalalogging.Logger

import scala.collection.mutable

/**
  * Class representing a service for managing by a storage for default state that is kept in RAM and use t-stream for checkpoint
  *
  * @param stateSaver  service for storing states
  * @param stateLoader service for loading states
  * @author Kseniya Mikhaleva
  */
class RAMStateService(stateSaver: StateSaverInterface,
                      stateLoader: StateLoaderInterface)
  extends IStateService {

  private val logger = Logger(this.getClass)

  /**
    * Provides key/value storage to keep state changes. It's used to do checkpoint of partial changes of state
    */
  protected val stateChanges: mutable.Map[String, (String, Any)] = mutable.Map[String, (String, Any)]()

  /**
    * Provides key/value storage to keep state
    */
  private val stateVariables: mutable.Map[String, Any] = stateLoader.loadLastState() match {
    case (id, state) =>
      stateSaver.lastFullStateID = id
      state
  }

  override def isExist(key: String): Boolean = {
    logger.info(s"Check whether a state variable: $key  exists or not.")
    stateVariables.contains(key)
  }

  override def get(key: String): Any = {
    logger.info(s"Get a state variable: $key.")
    stateVariables(key)
  }

  override def set(key: String, value: Any): Unit = {
    logger.info(s"Set a state variable: $key to $value.")
    stateVariables(key) = value
  }

  override def delete(key: String): Unit = {
    logger.info(s"Remove a state variable: $key.")
    stateVariables.remove(key)
  }

  override def clear(): Unit = {
    logger.info(s"Remove all state variables.")
    stateVariables.clear()
  }

  /**
    * Indicates that a state variable has been changed
    *
    * @param key   State variable name
    * @param value Value of the state variable
    */
  override def setChange(key: String, value: Any): Unit = {
    logger.info(s"Indicate that a state variable: $key value changed to $value.")
    stateChanges(key) = (setLiteral, value)
  }

  /**
    * Indicates that a state variable has been deleted
    *
    * @param key State variable name
    */
  override def deleteChange(key: String): Unit = {
    logger.info(s"Indicate that a state variable: $key with value: ${stateVariables(key)} deleted.")
    stateChanges(key) = (deleteLiteral, stateVariables(key))
  }

  /**
    * Indicates that all state variables have been deleted
    */
  override def clearChange(): Unit = {
    logger.info(s"Indicate that all state variables deleted.")
    stateVariables.foreach(x => deleteChange(x._1))
  }

  override def getNumberOfVariables: Int = {
    stateVariables.size
  }

  override def getAll: Map[String, Any] = stateVariables.toMap

  override def savePartialState(): Unit = {
    logger.debug(s"Do checkpoint of a part of state.")
    stateSaver.savePartialState(stateChanges, stateVariables)
    stateChanges.clear()
  }

  override def saveFullState(): Unit = {
    logger.debug(s"Do checkpoint of a full state.")
    stateSaver.saveFullState(stateChanges, stateVariables)
    stateChanges.clear()
  }
}
