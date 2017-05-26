package com.bwsw.sj.engine.input.config

import com.bwsw.sj.engine.core.config.EngineConfigNames.instance

/**
  * Names of configurations for application config
  */
object InputEngineConfigNames {
  val inputInstance = instance + ".input"
  val hosts = inputInstance + ".hosts"
  val entryPort = inputInstance + ".entry.port"
}
