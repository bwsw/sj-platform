package com.bwsw.sj.engine.output.benchmark

import java.util.logging.LogManager

import com.bwsw.sj.engine.output.OutputTaskRunner

/**
  * Created: 20/06/2016
  *
  * @author Kseniya Tomskikh
  */
object BenchmarkRunner extends App {
  LogManager.getLogManager.reset()
  OutputTaskRunner.main(Array())
}
