package com.bwsw.sj.crud.rest.instance

import com.fasterxml.jackson.annotation.JsonProperty

case class MarathonApplicationById(app: MarathonApplicationInfo)
case class MarathonApplicationInfo(id: String, env: Map[String, String], tasksRunning: Int,
                                   tasks: List[MarathonTask], lastTaskFailure: MarathonTaskFailure)
case class MarathonTask(id: String, host: String, ports: List[Int])
case class MarathonTaskFailure(host: String, message: String, state: String, timestamp: String)

case class MarathonInfo(@JsonProperty("marathon_config") marathonConfig: MarathonConfig)
case class MarathonConfig(master: String)