package com.bwsw.sj.common.utils

object SjStreamUtils {
  def clearStreamFromMode(streamName: String) = {
    streamName.replaceAll(s"/${EngineLiterals.splitStreamMode}|/${EngineLiterals.fullStreamMode}", "")
  }
}
