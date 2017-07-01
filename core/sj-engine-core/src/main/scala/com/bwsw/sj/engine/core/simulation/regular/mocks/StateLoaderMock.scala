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
package com.bwsw.sj.engine.core.simulation.regular.mocks

import com.bwsw.sj.common.engine.core.state.StateLoaderInterface

import scala.collection.mutable

/**
  * Mock for [[StateLoaderInterface]]
  *
  * @author Pavel Tomskikh
  */
class StateLoaderMock(lastStateId: Option[Long] = None, lastState: mutable.Map[String, Any] = mutable.Map.empty)
  extends StateLoaderInterface {

  /**
    * Allows getting last state. Needed for restoring after crashing
    *
    * @return (ID of the last state, state variables)
    */
  override def loadLastState(): (Option[Long], mutable.Map[String, Any]) = (lastStateId, lastState)
}
