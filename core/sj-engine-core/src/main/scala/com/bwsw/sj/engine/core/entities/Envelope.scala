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
package com.bwsw.sj.engine.core.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import com.bwsw.sj.common.utils.StreamLiterals
/**
 * Represents a message envelope that is received by an Executor for each message
 * that is received from a partition of a specific input stream [[StreamLiterals.types]]
 */

class Envelope extends Serializable {
  protected var streamType: String = _
  var stream: String = _
  var partition: Int = 0
  var tags: Array[String] = Array()
  var id: Long = 0

  @JsonIgnore()
  def isEmpty(): Boolean = Option(streamType).isEmpty
}