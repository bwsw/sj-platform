package com.bwsw.sj.engine.batch.module

import java.util.logging.LogManager

import com.bwsw.sj.engine.batch.BatchTaskRunner

object SjBatchModuleRunner extends App {
  LogManager.getLogManager.reset()
  BatchTaskRunner.main(Array())
}