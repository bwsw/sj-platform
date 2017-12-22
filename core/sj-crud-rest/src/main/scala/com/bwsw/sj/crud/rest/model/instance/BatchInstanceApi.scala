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
import com.bwsw.sj.common.si.model.instance.BatchInstance
import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import scaldi.Injector

/**
  * API entity for [[com.bwsw.sj.common.utils.EngineLiterals.batchStreamingType]] instance
  */
class BatchInstanceApi(name: String,
                       coordinationService: String,
                       val inputs: Array[String],
                       val outputs: Array[String],
                       description: String = RestLiterals.defaultDescription,
                       parallelism: Any = 1,
                       options: Map[String, Any] = Map(),
                       @JsonDeserialize(contentAs = classOf[Double]) perTaskCores: Option[Double] = Some(1),
                       @JsonDeserialize(contentAs = classOf[Int]) perTaskRam: Option[Int] = Some(1024),
                       jvmOptions: Map[String, String] = Map(),
                       nodeAttributes: Map[String, String] = Map(),
                       environmentVariables: Map[String, String] = Map(),
                       @JsonDeserialize(contentAs = classOf[Long]) performanceReportingInterval: Option[Long] = Some(60000),
                       @JsonDeserialize(contentAs = classOf[Int]) val window: Option[Int] = Some(1),
                       @JsonDeserialize(contentAs = classOf[Int]) val slidingInterval: Option[Int] = Some(1),
                       val startFrom: String = EngineLiterals.newestStartMode,
                       val stateManagement: String = EngineLiterals.noneStateMode,
                       @JsonDeserialize(contentAs = classOf[Int]) val stateFullCheckpoint: Option[Int] = Some(100),
                       @JsonDeserialize(contentAs = classOf[Long]) val eventWaitIdleTime: Option[Long] = Some(1000),
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
                 (implicit injector: Injector): BatchInstance = {
    val serializer = new JsonSerializer()

    new BatchInstance(
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
      inputs = Option(inputs).getOrElse(Array()),
      outputs = Option(outputs).getOrElse(Array()),
      window = window.getOrElse(1),
      slidingInterval = slidingInterval.getOrElse(1),
      startFrom = Option(startFrom).getOrElse(EngineLiterals.newestStartMode),
      stateManagement = Option(stateManagement).getOrElse(EngineLiterals.noneStateMode),
      stateFullCheckpoint = stateFullCheckpoint.getOrElse(100),
      eventWaitIdleTime = eventWaitIdleTime.getOrElse(1000l),
      creationDate = creationDate)
  }
}
