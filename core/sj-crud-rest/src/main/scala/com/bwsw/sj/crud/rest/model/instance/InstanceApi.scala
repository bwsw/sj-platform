package com.bwsw.sj.crud.rest.model.instance

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.instance._
import com.bwsw.sj.common.si.model.module.Specification
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


  def to(moduleType: String, moduleName: String, moduleVersion: String): Instance = {
    val serializer = new JsonSerializer()

    new Instance(
      name,
      description,
      parallelism,
      serializer.serialize(options),
      perTaskCores,
      perTaskRam,
      jvmOptions,
      nodeAttributes,
      coordinationService,
      environmentVariables,
      performanceReportingInterval,
      moduleName,
      moduleVersion,
      moduleType,
      getEngine(moduleType, moduleName, moduleVersion))
  }


  protected def getFilesMetadata(moduleType: String, moduleName: String, moduleVersion: String) = {
    val fileMetadataDAO = ConnectionRepository.getFileMetadataRepository
    fileMetadataDAO.getByParameters(Map("filetype" -> "module",
      "specification.name" -> moduleName,
      "specification.module-type" -> moduleType,
      "specification.version" -> moduleVersion)
    )
  }

  protected def getEngine(moduleType: String, moduleName: String, moduleVersion: String): String = {
    val filesMetadata = getFilesMetadata(moduleType, moduleName, moduleVersion)
    val fileMetadata = filesMetadata.head
    val specification = Specification.from(fileMetadata.specification)
    specification.engineName + "-" + specification.engineVersion
  }
}
