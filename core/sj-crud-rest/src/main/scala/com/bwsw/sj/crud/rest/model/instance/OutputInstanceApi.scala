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
import com.bwsw.sj.common.si.model.instance.OutputInstance
import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import scaldi.Injector

/**
  * API entity for [[com.bwsw.sj.common.utils.EngineLiterals.outputStreamingType]] instance
  */
class OutputInstanceApi(name: String,
                        coordinationService: String,
                        val checkpointMode: String,
                        @JsonProperty(required = true) val checkpointInterval: Long,
                        val input: String,
                        val output: String,
                        description: String = RestLiterals.defaultDescription,
                        parallelism: Any = 1,
                        options: Map[String, Any] = Map(),
                        @JsonDeserialize(contentAs = classOf[Double]) perTaskCores: Option[Double] = Some(1),
                        @JsonDeserialize(contentAs = classOf[Int]) perTaskRam: Option[Int] = Some(1024),
                        jvmOptions: Map[String, String] = Map(),
                        nodeAttributes: Map[String, String] = Map(),
                        environmentVariables: Map[String, String] = Map(),
                        @JsonDeserialize(contentAs = classOf[Long]) performanceReportingInterval: Option[Long] = Some(60000),
                        val startFrom: String = EngineLiterals.newestStartMode,
                        creationDate: String)
  extends InstanceApi(
    name,
    coordinationService,
    description,
    parallelism,
    options,
    perTaskCores,
    perTaskRam,
    jvmOptions,
    nodeAttributes,
    environmentVariables,
    performanceReportingInterval,
    creationDate = creationDate) {

  override def to(moduleType: String, moduleName: String, moduleVersion: String)
                 (implicit injector: Injector): OutputInstance = {
    val serializer = new JsonSerializer()

    new OutputInstance(
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
      getEngine(moduleType, moduleName, moduleVersion),
      checkpointMode,
      checkpointInterval,
      input,
      output,
      Option(startFrom).getOrElse(EngineLiterals.newestStartMode),
      creationDate = creationDate)
  }
}
