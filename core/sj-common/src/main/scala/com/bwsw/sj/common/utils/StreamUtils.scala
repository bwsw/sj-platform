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
    *
    * @param streamName name of stream that can contain a stream mode [[EngineLiterals.fullStreamMode]] or [[EngineLiterals.splitStreamMode]] is returned in all other cases
    */
  def getStreamMode(streamName: String): String = {
    if (streamName.contains(s"/${EngineLiterals.fullStreamMode}")) {
      EngineLiterals.fullStreamMode
    } else {
      EngineLiterals.splitStreamMode
    }
  }
}
