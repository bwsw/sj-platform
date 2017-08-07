/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.crud.rest.model.instance

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.instance._
import com.bwsw.sj.common.si.model.module.Specification
import com.bwsw.sj.common.utils.RestLiterals
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import scaldi.Injectable.inject
import scaldi.Injector

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
                  @JsonDeserialize(contentAs = classOf[Long]) val performanceReportingInterval: Option[Long] = Some(60000l),
                  val creationDate: String) {

  def to(moduleType: String, moduleName: String, moduleVersion: String)
        (implicit injector: Injector): Instance = {
    val serializer = new JsonSerializer()

    new Instance(
      name = name,
      description = Option(description).getOrElse(RestLiterals.defaultDescription),
      parallelism = Option(parallelism).getOrElse(1),
      options = serializer.serialize(Option(options).getOrElse(Map())),
      perTaskCores = perTaskCores.getOrElse(1),
      perTaskRam = perTaskRam.getOrElse(1024),
      jvmOptions = Option(jvmOptions).getOrElse(Map()),
      nodeAttributes = Option(nodeAttributes).getOrElse(Map()),
      coordinationService = coordinationService,
      environmentVariables = Option(environmentVariables).getOrElse(Map()),
      performanceReportingInterval = performanceReportingInterval.getOrElse(60000l),
      moduleName = moduleName,
      moduleVersion = moduleVersion,
      moduleType = moduleType,
      engine = getEngine(moduleType, moduleName, moduleVersion),
      creationDate = creationDate)
  }

  protected def getFilesMetadata(moduleType: String, moduleName: String, moduleVersion: String)
                                (implicit injector: Injector) = {
    val fileMetadataDAO = inject[ConnectionRepository].getFileMetadataRepository
    fileMetadataDAO.getByParameters(Map("filetype" -> "module",
      "specification.name" -> moduleName,
      "specification.module-type" -> moduleType,
      "specification.version" -> moduleVersion)
    )
  }

  protected def getEngine(moduleType: String, moduleName: String, moduleVersion: String)
                         (implicit injector: Injector): String = {
    val filesMetadata = getFilesMetadata(moduleType, moduleName, moduleVersion)
    val fileMetadata = filesMetadata.head
    val specification = Specification.from(fileMetadata.specification)
    specification.engineName + "-" + specification.engineVersion
  }
}
