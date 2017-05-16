package com.bwsw.sj.common.si.model.instance

import com.bwsw.sj.common.dal.model.instance.FrameworkStage
import com.bwsw.sj.common.utils.EngineLiterals

class Instance(val name: String,
               val description: String,
               val parallelism: Any,
               val options: Map[String, Any],
               val perTaskCores: Double,
               val perTaskRam: Int,
               val jvmOptions: Map[String, String],
               val nodeAttributes: Map[String, String],
               val coordinationService: String,
               val environmentVariables: Map[String, String],
               val performanceReportingInterval: Long,
               val moduleName: String,
               val moduleVersion: String,
               val moduleType: String,
               val engine: String,
               val restAddress: Option[String] = None,
               val stage: FrameworkStage = FrameworkStage(),
               val status: String = EngineLiterals.ready) {

}
