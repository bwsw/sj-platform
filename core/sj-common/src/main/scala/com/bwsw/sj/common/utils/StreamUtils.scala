package com.bwsw.sj.common.utils

object StreamUtils {
  /**
    * Returns a name of stream without stream mode
    * @param streamName name of stream containing stream mode [[EngineLiterals.splitStreamMode]] or [[EngineLiterals.fullStreamMode]]
    */
  def clearStreamFromMode(streamName: String): String = {
    streamName.replaceAll(s"/.*", "")
  }
}
