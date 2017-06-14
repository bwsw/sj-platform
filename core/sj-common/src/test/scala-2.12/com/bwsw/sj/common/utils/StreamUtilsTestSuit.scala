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

import org.scalatest.{FlatSpec, Matchers}

class StreamUtilsTestSuit extends FlatSpec with Matchers {
  it should s"clearStreamFromMode() method returns cleared stream name if the stream name contains a mode" in {
    //arrange
    val expectedStreamName = "name"
    val mode = "mode"
    val streamNameWithMode = expectedStreamName + "/" + mode

    //act
    val streamName = StreamUtils.clearStreamFromMode(streamNameWithMode)

    //assert
    streamNameWithMode should not equal expectedStreamName
    streamName shouldBe expectedStreamName
  }

  it should s"clearStreamFromMode() method returns the sane stream name if the stream name doesn't contain a mode" in {
    //arrange
    val expectedStreamName = "name"

    //act
    val streamName = StreamUtils.clearStreamFromMode(expectedStreamName)

    //assert
    streamName shouldBe expectedStreamName
  }

  it should s"getStreamMode() method returns an actual mode if the stream name has got '${EngineLiterals.fullStreamMode}' mode" in {
    //arrange
    val expectedStreamName = "name"
    val streamNameWithMode = expectedStreamName + "/" + EngineLiterals.fullStreamMode

    //act
    val mode = StreamUtils.getStreamMode(streamNameWithMode)

    //assert
    mode shouldBe EngineLiterals.fullStreamMode
  }

  it should s"getStreamMode() method returns an actual mode if the stream name has got '${EngineLiterals.splitStreamMode}' mode" in {
    //arrange
    val expectedStreamName = "name"
    val streamNameWithMode = expectedStreamName + "/" + EngineLiterals.splitStreamMode

    //act
    val mode = StreamUtils.getStreamMode(streamNameWithMode)

    //assert
    mode shouldBe EngineLiterals.splitStreamMode
  }

  it should s"getStreamMode() method returns '${EngineLiterals.splitStreamMode}' mode " +
    s"if the stream name has got a different mode from '${EngineLiterals.fullStreamMode}' or '${EngineLiterals.splitStreamMode}'" in {
    //arrange
    val expectedStreamName = "name"
    val someMode = "mode"
    val streamNameWithMode = expectedStreamName + "/" + someMode

    //act
    val mode = StreamUtils.getStreamMode(streamNameWithMode)

    //assert
    mode shouldBe EngineLiterals.splitStreamMode
  }
}
