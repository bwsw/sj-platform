package com.bwsw.sj.common.rest.entities

case class MarathonRequest(id: String,
                           cmd: String,
                           instances: Int,
                           env: Map[String, String],
                           uris: List[String],
                           backoffSeconds: Int = 1,
                           backoffFactor: Double = 1.15,
                           maxLaunchDelaySeconds: Int = 3600)