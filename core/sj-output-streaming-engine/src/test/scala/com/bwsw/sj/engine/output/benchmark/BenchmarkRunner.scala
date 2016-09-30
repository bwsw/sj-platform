package com.bwsw.sj.engine.output.benchmark

import java.util.logging.LogManager

import com.bwsw.sj.engine.output.OutputTaskRunner

/**
  *
  *
  * @author Kseniya Tomskikh
  */
object BenchmarkRunner extends App {
  LogManager.getLogManager.reset()
  OutputTaskRunner.main(Array())
}
