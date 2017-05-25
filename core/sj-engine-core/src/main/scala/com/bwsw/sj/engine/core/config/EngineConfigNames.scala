package com.bwsw.sj.engine.core.config

/**
  * Names of configurations for application config
  */
object EngineConfigNames {
  val engine = "engine"

  val instance = engine + ".instance"
  val instanceName = instance + ".name"
  val task = instance + ".task"
  val taskName = task + ".name"

  val agents = engine + ".agents"
  val agentsHost = agents + ".host"
  val agentsPorts = agents + ".ports"
}
