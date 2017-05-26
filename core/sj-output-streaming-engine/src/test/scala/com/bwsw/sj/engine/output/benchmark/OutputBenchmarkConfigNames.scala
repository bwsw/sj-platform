package com.bwsw.sj.engine.output.benchmark

import com.bwsw.sj.common.config.BenchmarkConfigNames.test


object OutputBenchmarkConfigNames {
  val esHosts = test + ".es.hosts"
  val jdbcHosts = test + ".jdbc.hosts"
  val restHosts = test + ".restful.hosts"

  val restPort = test + ".output.rest.port"
}
