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
package com.bwsw.sj.common.utils

object StreamUtils {
  /**
    * Returns a name of stream without stream mode
    *
    * @param streamName name of stream containing stream mode after forward slash (it should be [[EngineLiterals.splitStreamMode]] or [[EngineLiterals.fullStreamMode]])
    */
  def clearStreamFromMode(streamName: String): String = {
    streamName.replaceAll(s"/.*", "")
  }

  /**
    * Returns one of the following stream modes: [[EngineLiterals.streamModes]]
    */
  def getCorrectStreamMode(streamName: String): String = {
    if (streamName.contains(s"/${EngineLiterals.fullStreamMode}")) {
      EngineLiterals.fullStreamMode
    } else {
      EngineLiterals.splitStreamMode
    }
  }

  /**
    * Returns a stream mode. Used for validation
    *
    * @param streamName name of stream that contains any stream mode.
    */
  def getStreamMode(streamName: String): String = {
    val maybeMode = streamName.replaceAll(s".*/", "")
    if (maybeMode == streamName) EngineLiterals.splitStreamMode else maybeMode
  }
}
