package com.bwsw.sj.common.utils

object SjStreamUtils {
  def clearStreamFromMode(streamName: String): String = {
    streamName.replaceAll(s"/.*", "")
  }
}
