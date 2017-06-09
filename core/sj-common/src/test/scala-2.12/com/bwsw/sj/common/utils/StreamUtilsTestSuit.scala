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
