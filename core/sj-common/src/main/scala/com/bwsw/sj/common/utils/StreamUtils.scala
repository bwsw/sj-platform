package com.bwsw.sj.common.utils

object StreamUtils {
  def clearStreamFromMode(streamName: String): String = {
    streamName.replaceAll(s"/.*", "")
  }
}
