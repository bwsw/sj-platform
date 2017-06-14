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

import scala.collection.mutable.ArrayBuffer
import com.bwsw.sj.common.utils._

/**
  * Used in [[EngineLiterals.batchStreamingType]] engine to collect envelopes [[Envelope]] for each stream
  *
  * @param stream provides envelopes
  * @param tags   for user convenience to realize what kind of data are in a stream
  */
class Batch(val stream: String, val tags: Array[String]) extends Serializable {
  val envelopes: ArrayBuffer[Envelope] = ArrayBuffer()

  def copy(): Batch = {
    val copy = new Batch(this.stream, this.tags)
    this.envelopes.foreach(x => copy.envelopes += x)

    copy
  }
}