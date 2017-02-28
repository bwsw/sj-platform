package com.bwsw.sj.engine.windowed.module

import java.util.logging.LogManager

import com.bwsw.sj.engine.windowed.WindowedTaskRunner

object SjWindowedModuleRunner extends App {
  LogManager.getLogManager.reset()
  WindowedTaskRunner.main(Array())
}