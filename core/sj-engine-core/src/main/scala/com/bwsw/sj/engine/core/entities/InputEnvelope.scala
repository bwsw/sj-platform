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

import com.bwsw.sj.common.utils.{EngineLiterals, StreamLiterals}

/**
  * Provides a wrapper for t-stream transaction that is formed by [[EngineLiterals.inputStreamingType]] engine.
  *
  * @param key            a key for check on duplicate
  * @param outputMetadata information (stream -> partition) - where data should be placed
  * @param duplicateCheck whether a message should be checked on duplicate or not
  * @param data           message data
  * @tparam T type of data containing in a message
  */

class InputEnvelope[T <: AnyRef](var key: String,
                                 var outputMetadata: Array[(String, Int)],
                                 var duplicateCheck: Boolean,
                                 var data: T) extends Envelope {
  streamType = StreamLiterals.inputDummy
}