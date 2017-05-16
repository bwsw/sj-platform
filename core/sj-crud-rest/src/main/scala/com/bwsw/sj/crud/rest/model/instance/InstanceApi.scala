package com.bwsw.sj.crud.rest.model.instance

import com.bwsw.sj.common.utils.RestLiterals

class InstanceApi(val name: String,
                  val coordinationService: String,
                  val description: String = RestLiterals.defaultDescription,
                  val parallelism: Any = 1,
                  val options: Map[String, Any] = Map(),
                  val perTaskCores: Double = 1,
                  val perTaskRam: Int = 1024,
                  val jvmOptions: Map[String, String] = Map(),
                  val nodeAttributes: Map[String, String] = Map(),
                  val environmentVariables: Map[String, String] = Map(),
                  val performanceReportingInterval: Long = 60000) {

}
