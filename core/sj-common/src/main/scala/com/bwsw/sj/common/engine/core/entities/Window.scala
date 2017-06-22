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
package com.bwsw.sj.common.engine.core.entities

import com.bwsw.sj.common.utils.EngineLiterals

import scala.collection.mutable.ArrayBuffer

/**
  * Used in [[EngineLiterals.batchStreamingType]] engine to collect batches [[Batch]] for each stream
  *
  * @param stream stream name for which batches are collected
  */
class Window(val stream: String) {
  val batches: ArrayBuffer[Batch] = ArrayBuffer()

  def copy(): Window = {
    val copy = new Window(this.stream)
    this.batches.foreach(x => copy.batches += x)

    copy
  }
}
