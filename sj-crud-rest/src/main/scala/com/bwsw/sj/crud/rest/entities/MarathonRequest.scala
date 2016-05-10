package com.bwsw.sj.crud.rest.entities

/**
  * Protocol for creating and running application on mesos
  * Created: 27/04/2016
  *
  * @author Kseniya Tomskikh
  */
case class MarathonRequest(id: String,
                           cmd: String,
                           instances: Int,
                           env: Map[String, String],
                           uris: List[String])

/**
  * Protocol for creating and running application on mesos
  * Created: 27/04/2016
  *
  * @author Kseniya Tomskikh
  */
case class MarathonProtocol(id: String,
                            cmd: String,
                            args: List[String],
                            user: String,
                            env: Map[String, String],
                            instances: Int,
                            cpus: Double,
                            mem: Double,
                            disk: Int,
                            executor: String,
                            acceptedResourceRoles: List[String],
                            backoffFactor: Double,
                            backoffSeconds: Double,
                            uris: List[String],
                            constraints: List[List[String]],
                            container: Map[String, Any],
                            dependencies: List[String],
                            healthChecks: List[Map[String, Any]],
                            labels: Map[String, String],
                            maxLaunchDelaySeconds: Long,
                            ipAddress: Map[String, Any],
                            ports: List[Int],
                            requirePorts: Boolean,
                            portDefinitions: Map[String, Any],
                            fetch: Map[String, Any],
                            upgradeStrategy: Map[String, Double]
                           )