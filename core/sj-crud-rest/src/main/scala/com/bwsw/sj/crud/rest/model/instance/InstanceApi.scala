package com.bwsw.sj.crud.rest.model.instance

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.instance._
import com.bwsw.sj.common.si.model.module.Specification
import com.bwsw.sj.common.utils.RestLiterals
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

/**
  * API entity for instance
  */
class InstanceApi(val name: String,
                  val coordinationService: String,
                  val description: String = RestLiterals.defaultDescription,
                  val parallelism: Any = 1,
                  val options: Map[String, Any] = Map(),
                  @JsonDeserialize(contentAs = classOf[Double]) val perTaskCores: Option[Double] = Some(1),
                  @JsonDeserialize(contentAs = classOf[Int]) val perTaskRam: Option[Int] = Some(1024),
                  val jvmOptions: Map[String, String] = Map(),
                  val nodeAttributes: Map[String, String] = Map(),
                  val environmentVariables: Map[String, String] = Map(),
                  @JsonDeserialize(contentAs = classOf[Long]) val performanceReportingInterval: Option[Long] = Some(60000l)) {

  def to(moduleType: String, moduleName: String, moduleVersion: String): Instance = {
    val serializer = new JsonSerializer()

    new Instance(
      name,
      Option(description).getOrElse(RestLiterals.defaultDescription),
      Option(parallelism).getOrElse(1),
      serializer.serialize(Option(options).getOrElse(Map())),
      perTaskCores.getOrElse(1),
      perTaskRam.getOrElse(1024),
      Option(jvmOptions).getOrElse(Map()),
      Option(nodeAttributes).getOrElse(Map()),
      coordinationService,
      Option(environmentVariables).getOrElse(Map()),
      performanceReportingInterval.getOrElse(60000l),
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
