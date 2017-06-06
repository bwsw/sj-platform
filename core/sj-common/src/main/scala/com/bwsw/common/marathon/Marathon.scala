package com.bwsw.common.marathon

import com.fasterxml.jackson.annotation.JsonProperty

case class MarathonApplication(app: MarathonApplicationInfo)
case class MarathonApplicationInfo(id: String, env: Map[String, String], tasksRunning: Int,
                                   tasks: List[MarathonTask], lastTaskFailure: MarathonTaskFailure)
case class MarathonTask(id: String, host: String, ports: List[Int])
case class MarathonTaskFailure(host: String, message: String, state: String, timestamp: String)

case class MarathonInfo(@JsonProperty("marathon_config") marathonConfig: MarathonConfig)
case class MarathonConfig(master: String)

case class MarathonRequest(id: String,
                           cmd: String,
                           instances: Int,
                           env: Map[String, String],
                           uris: List[String],
                           backoffSeconds: Int = 1,
                           backoffFactor: Double = 1.15,
                           maxLaunchDelaySeconds: Int = 3600)

case class MarathonApplicationInstances(instances: Int)