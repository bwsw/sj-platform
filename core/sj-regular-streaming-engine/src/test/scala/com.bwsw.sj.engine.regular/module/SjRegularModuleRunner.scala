package com.bwsw.sj.engine.regular.module

import java.util.logging.LogManager

import com.bwsw.sj.engine.regular.RegularTaskRunner

object SjRegularModuleRunner extends App {
  LogManager.getLogManager.reset()
  RegularTaskRunner.main(Array())
}