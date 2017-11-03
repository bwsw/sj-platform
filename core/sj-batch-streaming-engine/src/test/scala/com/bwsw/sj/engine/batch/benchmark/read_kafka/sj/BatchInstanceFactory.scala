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
package com.bwsw.sj.engine.batch.benchmark.read_kafka.sj

import java.io.File
import java.util.Date

import com.bwsw.sj.common.dal.model.instance.{BatchInstanceDomain, ExecutionPlan}
import com.bwsw.sj.common.dal.model.module.SpecificationDomain
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.crud.rest.model.module.SpecificationApiCreator
import com.bwsw.sj.engine.core.testutils.benchmark.sj.InstanceFactory

/**
  * Provides method to create batch instance and module's specification
  *
  * @author Pavel Tomskikh
  */
object BatchInstanceFactory extends InstanceFactory[BatchInstanceDomain] {

  /**
    * Creates batch instance and module's specification
    *
    * @return (batch instance, specification)
    */
  override def create(module: File,
                      name: String,
                      coordinationService: ZKServiceDomain,
                      inputStream: StreamDomain,
                      outputStream: StreamDomain,
                      executionPlan: ExecutionPlan): (BatchInstanceDomain, SpecificationDomain) = {
    val specification = new SpecificationApiCreator().from(module).to.to
    val instance = new BatchInstanceDomain(
      name = name,
      moduleType = EngineLiterals.batchStreamingType,
      moduleName = specification.name,
      moduleVersion = specification.version,
      engine = specification.engineName + "-" + specification.engineVersion,
      coordinationService = coordinationService,
      status = EngineLiterals.started,
      inputs = Array(inputStream.name + "/split"),
      outputs = Array(outputStream.name),
      parallelism = 1,
      eventWaitIdleTime = 1,
      startFrom = EngineLiterals.oldestStartMode,
      executionPlan = executionPlan,
      performanceReportingInterval = Long.MaxValue,
      stateManagement = EngineLiterals.noneStateMode,
      stateFullCheckpoint = 0,
      creationDate = new Date())

    (instance, specification)
  }
}
